/**
 * Copyright 2012-2013 Lev Khomich
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
package org.semarglproject.jena.core.sink;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class to provide custom implementations of TripleSinks for Jena
 */
public abstract class AbstractJenaSink implements TripleSink {

    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Allows to specify Jena's model to sink triples to.
     * Subclass of {@link Model} must be passed as a value.
     */
    public static final String OUTPUT_MODEL_PROPERTY = "http://semarglproject.org/jena/properties/output-model";

    protected Model model;

    private final Map<String, Node> bnodeMap;

    protected AbstractJenaSink(Model model) {
        this.model = model;
        bnodeMap = new HashMap<String, Node>();
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
    public final void addNonLiteral(String subj, String pred, String obj) {
        addTriple(convertNonLiteral(subj), Node.createURI(pred), convertNonLiteral(obj));
    }

    @Override
    public final void addPlainLiteral(String subj, String pred, String content, String lang) {
        if (lang == null) {
            addTriple(convertNonLiteral(subj), Node.createURI(pred), Node.createLiteral(content));
        } else {
            addTriple(convertNonLiteral(subj), Node.createURI(pred),
                    Node.createLiteral(content, lang, false));
        }
    }

    @Override
    public final void addTypedLiteral(String subj, String pred, String content, String type) {
        Node literal = Node.createLiteral(content, "", new BaseDatatype(type));
        addTriple(convertNonLiteral(subj), Node.createURI(pred), literal);
    }

    @Override
    public boolean setProperty(String key, Object value) {
        if (OUTPUT_MODEL_PROPERTY.equals(key) && value instanceof Model) {
            model = (Model) value;
            return true;
        }
        return false;
    }

    /**
     * Callback method for handling Jena triples.
     * @param subj triple's subject
     * @param pred triple's predicate
     * @param obj triple's object
     */
    protected abstract void addTriple(Node subj, Node pred, Node obj);
}
