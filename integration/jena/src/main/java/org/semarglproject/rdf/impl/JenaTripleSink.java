/*
 * Copyright 2012 Lev Khomich
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

package org.semarglproject.rdf.impl;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.Lock;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

import java.util.HashMap;
import java.util.Map;

public final class JenaTripleSink implements TripleSink {

    private static final int DEFAULT_BATCH_SIZE = 512;

    public static final String OUTPUT_MODEL_PROPERTY = "http://semarglproject.org/jena/properties/output-model";

    private Model model;
    private final int batchSize;
    private int pos;
    private final Map<String, Node> bnodeMap;
    private Triple[] triples;

    public JenaTripleSink(Model model) {
        this(model, DEFAULT_BATCH_SIZE);
    }

    public JenaTripleSink(Model model, int batchSize) {
        super();
        this.model = model;
        this.batchSize = batchSize;
        bnodeMap = new HashMap<String, Node>();
    }

    private void newBatch() {
        triples = new Triple[batchSize];
        pos = 0;
    }

    private void addTriple(Node subj, Node pred, Node obj) {
        triples[pos++] = new Triple(subj, pred, obj);
        if (pos == batchSize) {
            model.enterCriticalSection(Lock.WRITE);
            model.getGraph().getBulkUpdateHandler().add(triples);
            model.leaveCriticalSection();
            newBatch();
        }
    }

    private Node getBNode(String bnode) {
        if (!bnodeMap.containsKey(bnode)) {
            bnodeMap.put(bnode, Node.createAnon());
        }
        return bnodeMap.get(bnode);
    }

    private Node convertNonLiteral(String arg) {
        if (arg.startsWith(RDF.BNODE_PREFIX)) {
            return getBNode(arg);
        }
        return Node.createURI(arg);
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        addTriple(convertNonLiteral(subj), Node.createURI(pred), convertNonLiteral(obj));
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        if (lang == null) {
            addTriple(convertNonLiteral(subj), Node.createURI(pred), Node.createLiteral(content));
        } else {
            addTriple(convertNonLiteral(subj), Node.createURI(pred),
                    Node.createLiteral(content, lang, false));
        }
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        Node literal = Node.createLiteral(content, "", new BaseDatatype(type));
        addTriple(convertNonLiteral(subj), Node.createURI(pred), literal);
    }

    @Override
    public void startStream() {
        newBatch();
    }

    @Override
    public void endStream() {
        if (pos == 0) {
            return;
        }
        Triple[] dummy = new Triple[pos];
        System.arraycopy(triples, 0, dummy, 0, pos);
        model.enterCriticalSection(Lock.WRITE);
        model.getGraph().getBulkUpdateHandler().add(dummy);
        model.leaveCriticalSection();
    }

    @Override
    public boolean setProperty(String key, Object value) {
        if (OUTPUT_MODEL_PROPERTY.equals(key) && value instanceof Model) {
            model = (Model) value;
            return true;
        }
        return false;
    }

    @Override
    public void setBaseUri(String baseUri) {
    }
}
