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

import org.semarglproject.sink.QuadSink;
import org.semarglproject.vocab.RDF;
import org.semarglproject.vocab.XSD;

import java.util.Deque;
import java.util.LinkedList;

final class JsonLdContentHandler {

    static final String CONTEXT = "@context";
    static final String GRAPH = "@graph";
    static final String LIST = "@list";
    static final String ID = "@id";
    static final String TYPE = "@type";
    static final String CONTAINER = "@container";
    static final String REVERSE = "@reverse";
    static final String LANGUAGE = "@language";
    static final String VALUE = "@value";
    static final String CONTAINER_LIST = "@container@list";

    private Deque<EvalContext> contextStack = new LinkedList<EvalContext>();
    private final DocumentContext dh = new DocumentContext();
    private EvalContext currentContext;

    public JsonLdContentHandler(QuadSink sink) {
        currentContext = EvalContext.createInitialContext(dh, sink);
    }

    public void onObjectStart() {
        if (CONTEXT.equals(currentContext.predicate)) {
            currentContext.parsingContext = true;
        } else if (GRAPH.equals(currentContext.predicate)) {
            String graph = currentContext.subject;
            contextStack.push(currentContext);
            currentContext = currentContext.initChildContext();
            if (graph != null && !graph.startsWith(RDF.BNODE_PREFIX)) {
                currentContext.graph = graph;
            }
        } else {
            contextStack.push(currentContext);
            currentContext = currentContext.initChildContext();
            if (contextStack.size() == 1) {
                currentContext.updateState(EvalContext.PARENT_SAFE);
            }
        }
    }

    public void onObjectEnd() {
        if (currentContext.objectLit != null) {
            if (currentContext.objectLitDt != null) {
                contextStack.peek().addTypedLiteral(currentContext.objectLit, currentContext.objectLitDt);
            } else {
                contextStack.peek().addPlainLiteral(currentContext.objectLit, currentContext.lang);
            }
        } else if (!currentContext.parsingContext) {
            addSubjectTypeDefinition(currentContext.objectLitDt);
            if (contextStack.size() > 1) {
                // TODO: check for property reordering issues
                addSubjectTypeDefinition(contextStack.peek().getDtMapping(contextStack.peek().predicate));
                contextStack.peek().addNonLiteral(contextStack.peek().predicate, currentContext.subject);
            }
        }
        if (currentContext.parsingContext && !contextStack.peek().parsingContext) {
            currentContext.updateState(EvalContext.CONTEXT_DECLARED);
            currentContext.parsingContext = false;
        } else {
            currentContext.updateState(EvalContext.ID_DECLARED | EvalContext.CONTEXT_DECLARED);
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
                currentContext.addListRest(RDF.NIL);
            } else {
                currentContext.subject = RDF.NIL;
            }
            // TODO: check for property reordering issues
            String dt = contextStack.peek().getDtMapping(contextStack.peek().predicate);
            if (CONTAINER_LIST.equals(dt)) {
                onObjectEnd();
            }
        }
    }

    public void onKey(String key) {
        currentContext.predicate = key;
    }

    public void onString(String value) {
        if (currentContext.parsingContext) {
            EvalContext parentContext = contextStack.peek();
            if (parentContext.parsingContext) {
                if (ID.equals(currentContext.predicate)) {
                    parentContext.defineIriMappingForPredicate(value);
                } else if (TYPE.equals(currentContext.predicate)) {
                    parentContext.defineDtMappingForPredicate(value);
                } else if (LANGUAGE.equals(currentContext.predicate)) {
                    parentContext.defineLangMappingForPredicate(value);
                } else if (CONTAINER.equals(currentContext.predicate)) {
                    parentContext.defineDtMappingForPredicate(CONTAINER + value);
                } else if (REVERSE.equals(currentContext.predicate)) {
                    parentContext.defineIriMappingForPredicate(value);
                    parentContext.defineDtMappingForPredicate(REVERSE);
                }
                return;
            } else if (!currentContext.isPredicateKeyword()) {
                currentContext.defineIriMappingForPredicate(value);
                return;
            }
        } else if (!currentContext.isPredicateKeyword() && currentContext.predicate != null) {
            // TODO: check for property reordering issues
            String dt = currentContext.getDtMapping(currentContext.predicate);
            if (CONTAINER_LIST.equals(dt)) {
                onObjectStart();
                onKey(LIST);
                onArrayStart();
                onString(value);
            } else {
                currentContext.addPlainLiteral(value, LANGUAGE);
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
                currentContext.subject = value;
                currentContext.updateState(EvalContext.ID_DECLARED);
            } else if (VALUE.equals(currentContext.predicate)) {
                currentContext.objectLit = value;
            } else if (LIST.equals(currentContext.predicate)) {
                if (currentContext.listTail == null) {
                    currentContext.listTail = currentContext.subject;
                    currentContext.addListFirst(value);
                } else {
                    currentContext.addListRest(dh.createBnode(false));
                    currentContext.addListFirst(value);
                }
            }
        }
    }

    private void addSubjectTypeDefinition(String dt) {
        if (dt == null || dt.charAt(0) == '@') {
            return;
        }
        currentContext.addNonLiteral(RDF.TYPE, dt);
    }

    public void onBoolean(boolean value) {
        currentContext.addTypedLiteral(Boolean.toString(value), XSD.BOOLEAN);
    }

    public void onNull() {
        if (CONTEXT.equals(currentContext.predicate)) {
            currentContext.nullify();
        } else {
            currentContext.defineIriMappingForPredicate(null);
        }
    }

    public void onNumber(double value) {
        currentContext.addTypedLiteral(Double.toString(value), XSD.DOUBLE);
    }

    public void onNumber(int value) {
        currentContext.addTypedLiteral(Integer.toString(value), XSD.INTEGER);
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
