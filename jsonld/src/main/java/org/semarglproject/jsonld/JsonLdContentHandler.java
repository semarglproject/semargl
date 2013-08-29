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
import org.semarglproject.vocab.JsonLd;
import org.semarglproject.vocab.RDF;
import org.semarglproject.vocab.XSD;

import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Handler class for JsonLdParser. Handles events in SAX-like manner.
 */
final class JsonLdContentHandler {

    private static final Pattern TERM_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+", Pattern.DOTALL);

    private Deque<EvalContext> contextStack = new LinkedList<EvalContext>();
    private final DocumentContext dh = new DocumentContext();
    private EvalContext currentContext;

    public JsonLdContentHandler(QuadSink sink) {
        currentContext = EvalContext.createInitialContext(dh, sink);
    }

    public void onObjectStart() {
        String graph = null;
        if (JsonLd.GRAPH_KEY.equals(currentContext.predicate)
                && (contextStack.size() > 1 || currentContext.hasNonGraphContextProps)) {
            graph = currentContext.subject;
        }
        contextStack.push(currentContext);
        currentContext = currentContext.initChildContext(graph);
        if (contextStack.size() == 1) {
            currentContext.updateState(EvalContext.PARENT_SAFE);
        }
        if (JsonLd.REVERSE_KEY.equals(currentContext.parent.predicate)) {
            currentContext.subject = currentContext.parent.subject;
            currentContext.reversed = true;
            currentContext.updateState(EvalContext.ID_DECLARED);
        }
    }

    public void onObjectEnd() {
        unwrap();
        if (currentContext.objectLit != null) {
            // ignore floating values
            if (contextStack.size() > 1 && !JsonLd.NULL.equals(currentContext.objectLit)) {
                if (currentContext.objectLitDt != null) {
                    currentContext.parent.addTypedLiteral(currentContext.objectLit, currentContext.objectLitDt);
                } else {
                    currentContext.parent.addPlainLiteral(currentContext.objectLit, currentContext.lang);
                }
            }
            // currentContext remove can be forced because literal nodes don't contain any unsafe triples to sink
            currentContext.updateState(EvalContext.PARENT_SAFE);
        } else if (!currentContext.isParsingContext()) {
            addSubjectTypeDefinition(currentContext.objectLitDt, currentContext.base);
            if (contextStack.size() > 1 && !currentContext.container) {
                // TODO: check for property reordering issues
                addSubjectTypeDefinition(currentContext.parent.getDtMapping(currentContext.parent.predicate),
                        currentContext.parent.base);
                if (!JsonLd.SET_KEY.equals(currentContext.parent.predicate) || currentContext.hasProps) {
                    currentContext.parent.addNonLiteral(currentContext.parent.predicate,
                            currentContext.subject, currentContext.base);
                }
            }
        }
        if (currentContext.isParsingContext()) {
            currentContext.parent.processContext(currentContext);
        }
        currentContext.updateState(EvalContext.ID_DECLARED | EvalContext.CONTEXT_DECLARED);
        currentContext = contextStack.pop();
    }

    public void onArrayStart() {
        currentContext.parsingArray = true;
    }

    public void onArrayEnd() {
        currentContext.parsingArray = false;
        if (JsonLd.LIST_KEY.equals(currentContext.predicate)) {
            if (currentContext.listTail != null) {
                currentContext.addListRest(RDF.NIL);
            } else {
                currentContext.subject = RDF.NIL;
                currentContext.container = false;
            }
        } else if (JsonLd.SET_KEY.equals(currentContext.predicate)) {
            currentContext.objectLit = JsonLd.NULL;
        } else if (currentContext.predicate != null) {
            String dt = currentContext.getDtMapping(currentContext.predicate);
            if (JsonLd.CONTAINER_LIST_KEY.equals(dt)) {
                try {
                    currentContext.addNonLiteral(currentContext.resolveMapping(currentContext.predicate), RDF.NIL,
                            currentContext.base);
                } catch (MalformedIriException e) {
                }
            }
        }
    }

    private void unwrap() {
        if (currentContext.parsingArray) {
            onArrayEnd();
        }
        if (!currentContext.wrapped) {
            return;
        }
        currentContext.wrapped = false;
        onObjectEnd();
    }

    public void onKey(String key) {
        unwrap();
        try {
            String mapping = currentContext.resolveMapping(key);
            try {
                // we need to go deeper... in case of keyword aliases in term definitions
                mapping = currentContext.resolveMapping(mapping);
            } catch (MalformedIriException e) {
            }
            if (mapping != null && mapping.charAt(0) == '@') {
                currentContext.predicate = mapping;
                if (mapping.equals(JsonLd.SET_KEY) || mapping.equals(JsonLd.LIST_KEY)) {
                    currentContext.container = true;
                }
            } else {
                currentContext.predicate = key;
            }
        } catch (MalformedIriException e) {
            currentContext.predicate = key;
        }
        if (JsonLd.SET_KEY.equals(currentContext.predicate) || JsonLd.LIST_KEY.equals(currentContext.predicate)) {
            onArrayStart();
        }
        if (!JsonLd.GRAPH_KEY.equals(currentContext.predicate) && !JsonLd.CONTEXT_KEY.equals(currentContext.predicate)) {
            currentContext.hasNonGraphContextProps = true;
            if (!currentContext.predicate.startsWith("@")) {
                currentContext.hasProps = true;
            }
        }
    }

    public void onString(String value) {
        if (currentContext.isParsingContext()) {
            EvalContext parentContext = currentContext.parent;
            if (parentContext.isParsingContext()) {
                if (JsonLd.ID_KEY.equals(currentContext.predicate)) {
                    parentContext.defineIriMappingForPredicate(value);
                } else if (JsonLd.TYPE_KEY.equals(currentContext.predicate)) {
                    parentContext.defineDtMappingForPredicate(value);
                } else if (JsonLd.LANGUAGE_KEY.equals(currentContext.predicate)) {
                    parentContext.defineLangMappingForPredicate(value);
                } else if (JsonLd.CONTAINER_KEY.equals(currentContext.predicate)) {
                    parentContext.defineDtMappingForPredicate(JsonLd.CONTAINER_KEY + value);
                } else if (JsonLd.REVERSE_KEY.equals(currentContext.predicate)) {
                    parentContext.defineIriMappingForPredicate(value);
                    parentContext.defineDtMappingForPredicate(JsonLd.REVERSE_KEY);
                }
                return;
            } else if (!currentContext.isPredicateKeyword()) {
                currentContext.defineIriMappingForPredicate(value);
                return;
            } else if (JsonLd.BASE_KEY.equals(currentContext.predicate)) {
                currentContext.base = value;
                return;
            }
        } else if (!currentContext.isPredicateKeyword() && currentContext.predicate != null) {
            // TODO: check for property reordering issues
            String dt = currentContext.getDtMapping(currentContext.predicate);
            if (JsonLd.CONTAINER_LIST_KEY.equals(dt)) {
                onObjectStart();
                onKey(JsonLd.LIST_KEY);
                onArrayStart();
                onString(value);
                currentContext.wrapped = true;
            } else {
                currentContext.addPlainLiteral(value, JsonLd.LANGUAGE_KEY);
            }
            return;
        }
        if (currentContext.isPredicateKeyword()) {
            if (JsonLd.TYPE_KEY.equals(currentContext.predicate)) {
                if (currentContext.parsingArray) {
                    addSubjectTypeDefinition(value, currentContext.base);
                } else {
                    currentContext.objectLitDt = value;
                }
            } else if (JsonLd.LANGUAGE_KEY.equals(currentContext.predicate)) {
                currentContext.lang = value;
            } else if (JsonLd.ID_KEY.equals(currentContext.predicate)) {
                if (TERM_PATTERN.matcher(value).matches()) {
                    // force terms to be not considered in @id
                    currentContext.subject = "./" + value;
                } else {
                    currentContext.subject = value;
                }
                currentContext.updateState(EvalContext.ID_DECLARED);
            } else if (JsonLd.VALUE_KEY.equals(currentContext.predicate)) {
                currentContext.objectLit = value;
            } else if (JsonLd.LIST_KEY.equals(currentContext.predicate) && isNotFloating()) {
                if (currentContext.listTail == null) {
                    currentContext.listTail = currentContext.subject;
                    currentContext.addListFirst(value);
                } else {
                    currentContext.addListRest(dh.createBnode(false));
                    currentContext.addListFirst(value);
                }
            } else if (JsonLd.SET_KEY.equals(currentContext.predicate) && isNotFloating()) {
                currentContext.addToSet(value);
            }
        }
    }

    private boolean isNotFloating() {
        return currentContext.parent != null && currentContext.parent.predicate != null && !currentContext.parent.predicate.startsWith("@");
    }

    private void addSubjectTypeDefinition(String dt, String base) {
        if (dt == null || dt.charAt(0) == '@') {
            return;
        }
        currentContext.addNonLiteral(RDF.TYPE, dt, base);
    }

    public void onBoolean(boolean value) {
        processTypedValue(Boolean.toString(value), XSD.BOOLEAN);
    }

    public void onNull() {
        if (JsonLd.CONTEXT_KEY.equals(currentContext.predicate)) {
            currentContext.nullify();
        } else if (JsonLd.VALUE_KEY.equals(currentContext.predicate)) {
            currentContext.objectLit = JsonLd.NULL;
        } else if (currentContext.isParsingContext()) {
            EvalContext parentContext = currentContext.parent;
            if (parentContext.isParsingContext()) {
                if (JsonLd.LANGUAGE_KEY.equals(currentContext.predicate)) {
                    parentContext.defineLangMappingForPredicate(JsonLd.NULL);
                }
            } else {
                if (JsonLd.LANGUAGE_KEY.equals(currentContext.predicate)) {
                    currentContext.lang = null;
                } else if (JsonLd.BASE_KEY.equals(currentContext.predicate)) {
                    currentContext.base = JsonLd.DOC_IRI;
                } else {
                    currentContext.defineIriMappingForPredicate(null);
                }
            }
        }
    }

    public void onNumber(double value) {
        processTypedValue(Double.toString(value), XSD.DOUBLE);
    }

    public void onNumber(int value) {
        processTypedValue(Integer.toString(value), XSD.INTEGER);
    }

    public void processTypedValue(String value, String defaultDt) {
        String predicateDt = currentContext.getDtMapping(currentContext.predicate);
        if (JsonLd.CONTAINER_LIST_KEY.equals(predicateDt)) {
            onObjectStart();
            onKey(JsonLd.LIST_KEY);
            onArrayStart();
            currentContext.wrapped = true;
        } else if (JsonLd.CONTAINER_SET_KEY.equals(predicateDt)) {
            onObjectStart();
            onKey(JsonLd.SET_KEY);
            onArrayStart();
            currentContext.wrapped = true;
        }

        String dt = currentContext.getDtMapping(currentContext.predicate);
        if (dt == null) {
            dt = defaultDt;
        }

        if (JsonLd.LIST_KEY.equals(currentContext.predicate) && isNotFloating()) {
            if (currentContext.listTail == null) {
                currentContext.listTail = currentContext.subject;
                currentContext.addListFirst(value, dt);
            } else {
                currentContext.addListRest(dh.createBnode(false));
                currentContext.addListFirst(value, dt);
            }
        } else if (JsonLd.SET_KEY.equals(currentContext.predicate) && isNotFloating()) {
            currentContext.addToSet(value, dt);
        } else {
            currentContext.addTypedLiteral(value, dt);
        }
    }

    public void clear() {
        dh.clear();
        contextStack.clear();
        currentContext = null;
    }

    public void setBaseUri(String baseUri) {
        dh.iri = baseUri;
    }
}
