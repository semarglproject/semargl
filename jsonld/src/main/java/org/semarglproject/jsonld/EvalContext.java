/**
 * Copyright 2012-2013 the Semargl contributors. See AUTHORS for more details.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.semarglproject.jsonld;

import org.semarglproject.ri.MalformedCurieException;
import org.semarglproject.ri.MalformedIriException;
import org.semarglproject.ri.RIUtils;
import org.semarglproject.sink.QuadSink;
import org.semarglproject.vocab.JsonLd;
import org.semarglproject.vocab.RDF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

final class EvalContext {

    static final int ID_DECLARED = 1;
    static final int CONTEXT_DECLARED = 2;
    static final int PARENT_SAFE = 4;
    static final int SAFE_TO_SINK_TRIPLES = ID_DECLARED | CONTEXT_DECLARED | PARENT_SAFE;

    String graph;
    String subject;
    String predicate;
    String lang;
    String objectLit;
    String objectLitDt;
    String listTail;
    boolean parsingArray;
    boolean nullified;

    EvalContext parent;
    private int state;

    private final QuadSink sink;
    private final DocumentContext documentContext;
    private final Map<String, String> iriMappings = new TreeMap<String, String>();
    private final Map<String, String> dtMappings = new TreeMap<String, String>();
    private final Map<String, String> langMappings = new TreeMap<String, String>();

    private final Collection<EvalContext> children = new ArrayList<EvalContext>();

    private final Queue<String> nonLiteralQueue = new LinkedList<String>();
    private final Queue<String> plainLiteralQueue = new LinkedList<String>();
    private final Queue<String> typedLiteralQueue = new LinkedList<String>();

    private EvalContext(DocumentContext documentContext, QuadSink sink, EvalContext parent) {
        this.sink = sink;
        this.parent = parent;
        this.documentContext = documentContext;
    }

    static EvalContext createInitialContext(DocumentContext documentContext, QuadSink sink) {
        EvalContext initialContext = new EvalContext(documentContext, sink, null);
        initialContext.subject = documentContext.base;
        initialContext.state = SAFE_TO_SINK_TRIPLES;
        return initialContext;
    }

    EvalContext initChildContext() {
        EvalContext child = new EvalContext(documentContext, sink, this);
        child.lang = this.lang;
        child.subject = documentContext.createBnode(false);
        children.add(child);
        return child;
    }

    void nullify() {
        iriMappings.clear();
        dtMappings.clear();
        langMappings.clear();
        lang = null;
        nullified = true;
    }

    boolean isPredicateKeyword() {
        return predicate.charAt(0) == '@';
    }

    void defineIriMappingForPredicate(String value) {
        iriMappings.put(predicate, value);
        if (!dtMappings.containsKey(predicate)) {
            dtMappings.put(predicate, null);
        }
        if (!langMappings.containsKey(predicate)) {
            langMappings.put(predicate, null);
        }
    }

    void defineDtMappingForPredicate(String value) {
        dtMappings.put(predicate, value);
    }

    String getDtMapping(String value) {
        if (dtMappings.containsKey(value)) {
            return dtMappings.get(value);
        }
        if (!nullified && parent != null) {
            return parent.getDtMapping(value);
        }
        return null;
    }

    public void defineLangMappingForPredicate(String value) {
        langMappings.put(predicate, value);
    }

    private String getLangMapping(String value) {
        if (langMappings.containsKey(value)) {
            return langMappings.get(value);
        }
        if (!nullified && parent != null) {
            return parent.getLangMapping(value);
        }
        return null;
    }

    void updateState(int state) {
        this.state |= state;
        if (this.state == SAFE_TO_SINK_TRIPLES) {
            for (EvalContext child : children.toArray(new EvalContext[children.size()])) {
                child.updateState(PARENT_SAFE);
            }
            if (children.isEmpty()) {
                sinkUnsafeTriples();
            }
        }
    }

    private void sinkUnsafeTriples() {
        try {
            if (!subject.startsWith(RDF.BNODE_PREFIX)) {
                subject = resolveCurieOrIri(subject, false);
            }
            if (graph != null) {
                graph = resolve(graph, false);
            }
        } catch (MalformedIriException e) {
            nonLiteralQueue.clear();
            plainLiteralQueue.clear();
            typedLiteralQueue.clear();
        }
        while (!nonLiteralQueue.isEmpty()) {
            addNonLiteralUnsafe(nonLiteralQueue.poll(), nonLiteralQueue.poll());
        }
        while (!plainLiteralQueue.isEmpty()) {
            addPlainLiteralUnsafe(plainLiteralQueue.poll(), plainLiteralQueue.poll(), plainLiteralQueue.poll());
        }
        while (!typedLiteralQueue.isEmpty()) {
            addTypedLiteralUnsafe(typedLiteralQueue.poll(), typedLiteralQueue.poll(), typedLiteralQueue.poll());
        }
        if (parent != null) {
            parent.children.remove(this);
            parent = null;
        }
    }

    // TODO: check for property reordering issues
    public void addListFirst(String object) {
        if (listTail.equals(subject)) {
            if (state == SAFE_TO_SINK_TRIPLES) {
                addPlainLiteralUnsafe(RDF.FIRST, object, lang);
            } else {
                plainLiteralQueue.offer(RDF.FIRST);
                plainLiteralQueue.offer(object);
                plainLiteralQueue.offer(lang);
            }
        } else {
            sink.addPlainLiteral(listTail, RDF.FIRST, object, lang, graph);
        }
    }

    // TODO: check for property reordering issues
    public void addListRest(String object) {
        if (listTail.equals(subject)) {
            if (state == SAFE_TO_SINK_TRIPLES) {
                addNonLiteralUnsafe(RDF.REST, object);
            } else {
                nonLiteralQueue.offer(RDF.REST);
                nonLiteralQueue.offer(object);
            }
        } else {
            sink.addNonLiteral(listTail, RDF.REST, object, graph);
        }
        listTail = object;
    }

    void addNonLiteral(String predicate, String object) {
        if (state == SAFE_TO_SINK_TRIPLES) {
            addNonLiteralUnsafe(predicate, object);
        } else {
            nonLiteralQueue.offer(predicate);
            nonLiteralQueue.offer(object);
        }
    }

    private void addNonLiteralUnsafe(String predicate, String object) {
        try {
            if (object == null) {
                return;
            }
            if (!object.startsWith(RDF.BNODE_PREFIX)) {
                object = resolve(object, false);
            }
            boolean reversed = dtMappings.containsKey(predicate)
                    && JsonLd.REVERSE_KEY.equals(dtMappings.get(predicate));
            String resolvedPredicate = resolve(predicate, true);
            if (reversed) {
                sink.addNonLiteral(object, resolvedPredicate, subject, graph);
            } else {
                sink.addNonLiteral(subject, resolvedPredicate, object, graph);
            }
        } catch (MalformedIriException e) {
        }

    }

    void addPlainLiteral(String object, String lang) {
        if (state == SAFE_TO_SINK_TRIPLES) {
            addPlainLiteralUnsafe(predicate, object, lang);
        } else {
            plainLiteralQueue.offer(predicate);
            plainLiteralQueue.offer(object);
            plainLiteralQueue.offer(lang);
        }
    }

    private void addPlainLiteralUnsafe(String predicate, String object, String lang) {
        try {
            String dt = getDtMapping(predicate);
            if (dt != null) {
                if (JsonLd.ID_KEY.equals(dt)) {
                    addNonLiteralUnsafe(predicate, object);
                    return;
                } else if (!dt.startsWith("@")) {
                    addTypedLiteralUnsafe(predicate, object, dt);
                    return;
                }
            }
            String resolvedLang = lang;
            if (JsonLd.LANGUAGE_KEY.equals(lang)) {
                resolvedLang = getLangMapping(predicate);
                if (JsonLd.NULL.equals(resolvedLang)) {
                    resolvedLang = null;
                } else if (resolvedLang == null) {
                    resolvedLang = this.lang;
                }
            }
            boolean reversed = dtMappings.containsKey(predicate)
                    && JsonLd.REVERSE_KEY.equals(dtMappings.get(predicate));
            if (reversed) {
                sink.addNonLiteral(object, resolve(predicate, true), subject, graph);
            } else {
                sink.addPlainLiteral(subject, resolve(predicate, true), object, resolvedLang, graph);
            }
        } catch (MalformedIriException e) {
        }
    }

    void addTypedLiteral(String object, String dt) {
        if (state == SAFE_TO_SINK_TRIPLES) {
            addTypedLiteralUnsafe(predicate, object, dt);
        } else {
            typedLiteralQueue.offer(predicate);
            typedLiteralQueue.offer(object);
            typedLiteralQueue.offer(dt);
        }
    }

    private void addTypedLiteralUnsafe(String predicate, String object, String dt) {
        try {
            boolean reversed = dtMappings.containsKey(predicate)
                    && JsonLd.REVERSE_KEY.equals(dtMappings.get(predicate));
            if (reversed) {
                sink.addNonLiteral(object, resolve(predicate, true), subject, graph);
            } else {
                sink.addTypedLiteral(subject, resolve(predicate, true), object, resolve(dt, true), graph);
            }
        } catch (MalformedIriException e) {
        }
    }

    private String resolve(String value, boolean ignoreRelIri) throws MalformedIriException {
        if (value == null || value.isEmpty()) {
            throw new MalformedIriException("Empty predicate or datatype found");
        }
        try {
            String mapping = resolveMapping(value);
            if (mapping != null && mapping.charAt(0) != '@') {
                return resolveCurieOrIri(mapping, false);
            }
        } catch (MalformedIriException e) {
        }
        return resolveCurieOrIri(value, ignoreRelIri);
    }

    String resolveMapping(String value) throws MalformedIriException {
        if (iriMappings.containsKey(value)) {
            return iriMappings.get(value);
        }
        if (!nullified && parent != null) {
            return parent.resolveMapping(value);
        }
        throw new MalformedIriException("Can't resolve term " + value);
    }

    private String resolveCurieOrIri(String curie, boolean ignoreRelIri) throws MalformedIriException {
        if (!ignoreRelIri && (curie == null || curie.isEmpty())) {
            return documentContext.resolveIri(curie);
        }

        int delimPos = curie.indexOf(':');
        if (delimPos == -1) {
            if (ignoreRelIri) {
                throw new MalformedCurieException("CURIE with no prefix (" + curie + ") found");
            }
            return documentContext.resolveIri(curie);
        }

        String prefix = curie.substring(0, delimPos);
        if (prefix.equals("_")) {
            throw new MalformedCurieException("CURIE with invalid prefix (" + curie + ") found");
        }

        try {
            String prefixUri = resolveMapping(prefix);
            if (prefixUri != null) {
                return prefixUri + curie.substring(delimPos + 1);
            } else if (RIUtils.isIri(curie)) {
                return curie;
            }
        } catch (MalformedIriException e) {
            if (RIUtils.isIri(curie)) {
                return curie;
            }
        }
        throw new MalformedIriException("Malformed IRI: " + curie);
    }

    public boolean isParsingContext() {
        return parent != null && (JsonLd.CONTEXT_KEY.equals(parent.predicate)
                || parent.parent != null && JsonLd.CONTEXT_KEY.equals(parent.parent.predicate));
    }

    public void processContext(EvalContext context) {
        iriMappings.putAll(context.iriMappings);
        dtMappings.putAll(context.dtMappings);
        langMappings.putAll(context.langMappings);
        lang = context.lang;
        updateState(CONTEXT_DECLARED);
        children.remove(context);
    }
}
