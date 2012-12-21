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

package org.semarglproject.sesame.core.sink;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

/**
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public class SesameSink implements TripleSink {

    public static final String RDF_HANDLER_PROPERTY = "http://semarglproject.org/sesame/properties/rdf-handler";
    public static final String VALUE_FACTORY_PROPERTY = "http://semarglproject.org/sesame/properties/value-factory";

    private ValueFactory valueFactory;
    private RDFHandler handler;

    private SesameSink(RDFHandler handler) {
        this.valueFactory = ValueFactoryImpl.getInstance();
        this.handler = handler;
    }

    public static TripleSink to(RDFHandler handler) {
        return new SesameSink(handler);
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
    public boolean setProperty(String key, Object value) {
        if (RDF_HANDLER_PROPERTY.equals(key) && value instanceof RDFHandler) {
            handler = (RDFHandler) value;
        } else if (VALUE_FACTORY_PROPERTY.equals(key) && value instanceof ValueFactory) {
            valueFactory = (ValueFactory) value;
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void setBaseUri(String baseUri) {
    }
}
