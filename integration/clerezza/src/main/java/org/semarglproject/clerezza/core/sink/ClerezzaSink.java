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

package org.semarglproject.clerezza.core.sink;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

import java.util.HashMap;
import java.util.Map;

public class ClerezzaSink implements TripleSink {

    public static final String OUTPUT_GRAPH_PROPERTY = "http://semarglproject.org/clerezza/properties/output-graph";

    private MGraph graph;
    private final Map<String, BNode> bnodeMap;

    private ClerezzaSink(MGraph graph) {
        super();
        this.graph = graph;
        bnodeMap = new HashMap<String, BNode>();
    }

    public static TripleSink connect(MGraph graph) {
        return new ClerezzaSink(graph);
    }

    private BNode getBNode(String bnode) {
        if (!bnodeMap.containsKey(bnode)) {
            bnodeMap.put(bnode, new BNode());
        }
        return bnodeMap.get(bnode);
    }

    private NonLiteral convertNonLiteral(String arg) {
        if (arg.startsWith(RDF.BNODE_PREFIX)) {
            return getBNode(arg);
        }
        return new UriRef(arg);
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        graph.add(new TripleImpl(convertNonLiteral(subj), new UriRef(pred), convertNonLiteral(obj)));
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        Literal lit;
        if (lang == null || lang.equals("")) {
            lit = new PlainLiteralImpl(content);
        } else {
            lit = new PlainLiteralImpl(content, new Language(lang));
        }
        graph.add(new TripleImpl(convertNonLiteral(subj), new UriRef(pred), lit));
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        graph.add(new TripleImpl(convertNonLiteral(subj), new UriRef(pred), new TypedLiteralImpl(
                content, new UriRef(type))));
    }

    @Override
    public void startStream() throws ParseException {
    }

    @Override
    public void endStream() throws ParseException {
    }

    @Override
    public boolean setProperty(String key, Object value) {
        if (OUTPUT_GRAPH_PROPERTY.equals(key) && value instanceof MGraph) {
            graph = (MGraph) value;
            return true;
        }
        return false;
    }

    @Override
    public void setBaseUri(String baseUri) {
    }
}
