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

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.vocab.RDF;

/**
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public class SesameTripleSink implements TripleSink {
    private ValueFactory valueFactory;
    private RDFHandler handler;

    public SesameTripleSink(ValueFactory model, RDFHandler handler) {
        super();
        this.valueFactory = model;
        this.handler = handler;
    }

    private BNode getBNode(String bnode) {
        return valueFactory.createBNode(bnode.substring(2));
    }

    private Resource convertNonLiteral(String arg) {
        if (arg.startsWith(RDF.BNODE_PREFIX)) {
            return getBNode(arg);
        }
        return valueFactory.createURI(arg);
    }

    private void addTriple(Resource subject, URI predicate, Value object){
        try {
            handler.handleStatement(valueFactory.createStatement(subject, predicate, object));
        } catch(RDFHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        addTriple(convertNonLiteral(subj), valueFactory.createURI(pred), convertNonLiteral(obj));
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        if (lang == null) {
            addTriple(convertNonLiteral(subj), valueFactory.createURI(pred), valueFactory.createLiteral(content));
        } else {
            addTriple(convertNonLiteral(subj), valueFactory.createURI(pred),
                    valueFactory.createLiteral(content, lang));
        }
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        Literal literal = valueFactory.createLiteral(content, valueFactory.createURI(type));
        addTriple(convertNonLiteral(subj), valueFactory.createURI(pred), literal);
    }

    @Override
    public void startStream() {
        try {
            handler.startRDF();
        } catch(RDFHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void endStream() {
        try {
            handler.endRDF();
        } catch(RDFHandlerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setBaseUri(String baseUri) {
    }

}
