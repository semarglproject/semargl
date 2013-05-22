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

import org.semarglproject.ri.MalformedIriException;
import org.semarglproject.sink.QuadSink;
import org.semarglproject.vocab.RDF;
import org.semarglproject.vocab.XSD;

import java.util.Deque;
import java.util.LinkedList;

final class JsonLdContentHandler {

    private static final String CONTEXT = "@context";
    private static final String GRAPH = "@graph";
    private static final String LIST = "@list";
    private static final String ID = "@id";
    private static final String TYPE = "@type";
    private static final String CONTAINER = "@container";
    private static final String REVERSE = "@reverse";
    private static final String LANGUAGE = "@language";
    private static final String VALUE = "@value";
    private static final String CONTAINER_LIST = "@container@list";

    private final QuadSink sink;

    private Deque<EvalContext> contextStack = new LinkedList<EvalContext>();
    private final DocumentContext dh;
    private EvalContext currentContext;

    public JsonLdContentHandler(QuadSink sink) {
        this.sink = sink;
        dh = new DocumentContext();
        currentContext = EvalContext.createInitialContext(dh);
    }

    public void onObjectStart() {
        if (CONTEXT.equals(currentContext.predicate)) {
            currentContext.parsingContext = true;
        } else if (GRAPH.equals(currentContext.predicate)) {
            String graph = currentContext.subject;
            contextStack.push(currentContext);
            currentContext = currentContext.initChildContext();
            if (!graph.startsWith(RDF.BNODE_PREFIX)) {
                currentContext.graph = graph;
            }
        } else {
            contextStack.push(currentContext);
            currentContext = currentContext.initChildContext();
        }
    }

    public void onObjectEnd() {
        if (currentContext.objectLit != null) {
            if (currentContext.objectLitDt != null) {
                addTypedLiteral(contextStack.peek(), currentContext.objectLit, currentContext.objectLitDt);
            } else {
                addPlainLiteral(contextStack.peek(), currentContext.objectLit);
            }
        } else if (!currentContext.parsingContext) {
            addSubjectTypeDefinition(currentContext.objectLitDt);
            if (contextStack.size() > 1 && contextStack.peek().predicate != null) {
                addSubjectTypeDefinition(contextStack.peek().getPredicateDtMapping());
                addNonLiteral(contextStack.peek(), currentContext.subject);
            }
        }
        if (currentContext.parsingContext) {
            currentContext.parsingContext = false;
            if (contextStack.peek().parsingContext) {
                currentContext = contextStack.pop();
            }
        } else {
            currentContext = contextStack.pop();
        }
    }

    public void onArrayStart() {
        currentContext.parsingArray = true;
    }

    public void onArrayEnd() {
        currentContext.parsingArray = false;
        if (LIST.equals(currentContext.predicate)) {
            if (currentContext.listTail != null) {
                sink.addNonLiteral(currentContext.listTail, RDF.REST, RDF.NIL, currentContext.graph);
            } else {
                currentContext.subject = RDF.NIL;
            }
            String dt = contextStack.peek().getPredicateDtMapping();
            if (CONTAINER_LIST.equals(dt)) {
                onObjectEnd();
            }
        }
    }

    public void onString(String value) {
        if (currentContext.parsingContext) {
            if (contextStack.peek().parsingContext) {
                if (ID.equals(currentContext.predicate)) {
                    contextStack.peek().defineTerm(value);
                } else if (TYPE.equals(currentContext.predicate)) {
                    contextStack.peek().setPredicateDtMapping(value);
                } else if (CONTAINER.equals(currentContext.predicate)) {
                    contextStack.peek().setPredicateDtMapping(CONTAINER + value);
                } else if (REVERSE.equals(currentContext.predicate)) {
                }
                return;
            } else if (!currentContext.isPredicateKeyword()) {
                currentContext.addIriMapping(currentContext.predicate, value);
                return;
            }
        } else if (!currentContext.isPredicateKeyword() && currentContext.predicate != null) {
            String dt = currentContext.getPredicateDtMapping();
            if (ID.equals(dt)) {
                addNonLiteral(currentContext, value);
            } else if (CONTAINER_LIST.equals(dt)) {
                onObjectStart();
                onKey(LIST);
                onArrayStart();
                onString(value);
            } else if (dt != null && dt.charAt(0) != '@') {
                addTypedLiteral(currentContext, value, dt);
            } else {
                addPlainLiteral(currentContext, value);
            }
            return;
        }
        if (currentContext.isPredicateKeyword()) {
            if (TYPE.equals(currentContext.predicate)) {
                if (currentContext.parsingArray) {
                    addSubjectTypeDefinition(value);
                } else {
                    currentContext.objectLitDt = value;
                }
            } else if (LANGUAGE.equals(currentContext.predicate)) {
                currentContext.lang = value;
            } else if (ID.equals(currentContext.predicate)) {
                try {
                    currentContext.subject = currentContext.resolveAboutOrResource(value);
                } catch (MalformedIriException e) {
                }
            } else if (VALUE.equals(currentContext.predicate)) {
                currentContext.objectLit = value;
            } else if (LIST.equals(currentContext.predicate)) {
                addToList(value);
            }
        }
    }

    private void addToList(String value) {
        if (currentContext.listTail == null) {
            currentContext.listTail = currentContext.subject;
            sink.addPlainLiteral(currentContext.listTail, RDF.FIRST, value, currentContext.lang, currentContext.graph);
        } else {
            String prevNode = currentContext.listTail;
            currentContext.listTail = dh.createBnode(false);
            sink.addNonLiteral(prevNode, RDF.REST, currentContext.listTail, currentContext.graph);
            sink.addPlainLiteral(currentContext.listTail, RDF.FIRST, value, currentContext.lang, currentContext.graph);
        }
    }

    public void onKey(String key) {
        currentContext.setPredicate(key);
    }

    private void addNonLiteral(EvalContext context, String object) {
        if (context.isPredicateValid()) {
            sink.addNonLiteral(context.subject, context.predicate, object, currentContext.graph);
        }
    }

    private void addSubjectTypeDefinition(String dt) {
        if (dt == null || dt.charAt(0) == '@') {
            return;
        }
        try {
            sink.addNonLiteral(currentContext.subject, RDF.TYPE,
                    currentContext.resolvePredOrDatatype(dt), currentContext.graph);
        } catch (MalformedIriException e) {
        }
    }

    private void addPlainLiteral(EvalContext context, String object) {
        if (context.isPredicateValid()) {
            sink.addPlainLiteral(context.subject, context.predicate, object, currentContext.lang, currentContext.graph);
        }
    }

    private void addTypedLiteral(EvalContext context, String object, String dt) {
        try {
            if (context.isPredicateValid()) {
                sink.addTypedLiteral(context.subject, context.predicate,
                        object, currentContext.resolvePredOrDatatype(dt), currentContext.graph);
            }
        } catch (MalformedIriException e) {
        }
    }

    public void onBoolean(boolean value) {
        addTypedLiteral(currentContext, Boolean.toString(value), XSD.BOOLEAN);
    }

    public void onNull() {
//        addNonLiteral(currentContext, RDF.NIL);
    }

    public void onNumber(double value) {
        addTypedLiteral(currentContext, Double.toString(value), XSD.DOUBLE);
    }

    public void onNumber(int value) {
        addTypedLiteral(currentContext, Integer.toString(value), XSD.INTEGER);
    }

    public void clear() {
        dh.clear();
        contextStack.clear();
        currentContext = null;
    }

    public void setBaseUri(String baseUri) {
        dh.base = baseUri;
    }
}
