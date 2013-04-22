/**
 * Copyright 2012-2013 Lev Khomich
 * Copyright 2012-2013 Peter Ansell
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

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

/**
 * Implementation if {@link TripleSink} which feeds triples from Semargl's pipeline to Sesame's {@link RDFHandler}.
 * <p>
 *     List of supported options:
 *     <ul>
 *         <li>{@link #RDF_HANDLER_PROPERTY}</li>
 *         <li>{@link #VALUE_FACTORY_PROPERTY}</li>
 *     </ul>
 * </p>
 *
 * @author Peter Ansell p_ansell@yahoo.com
 * @author Lev Khomich levkhomich@gmail.com
 *
 */
public class SesameSink implements TripleSink {

    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Allows to specify Sesame's RDF handler.
     * Subclass of {@link RDFHandler} must be passed as a value.
     */
    public static final String RDF_HANDLER_PROPERTY = "http://semarglproject.org/sesame/properties/rdf-handler";

    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Allows to specify Sesame's value factory used to generate statemets.
     * Subclass of {@link ValueFactory} must be passed as a value.
     */
    public static final String VALUE_FACTORY_PROPERTY = "http://semarglproject.org/sesame/properties/value-factory";

    protected RDFHandler handler;
    protected ValueFactory valueFactory;

    protected SesameSink(RDFHandler handler) {
        this.valueFactory = ValueFactoryImpl.getInstance();
        this.handler = handler;
    }

    /**
     * Instantiates sink for specified Sesame {@link RDFHandler}
     * @param handler RDFHandler to sink triples to
     * @return new instance of Sesame sink
     */
    public static TripleSink connect(RDFHandler handler) {
        return new SesameSink(handler);
    }

    private Resource convertNonLiteral(String arg) {
        if (arg.startsWith(RDF.BNODE_PREFIX)) {
            return valueFactory.createBNode(arg.substring(2));
        }
        return valueFactory.createURI(arg);
    }

    @Override
    public final void addNonLiteral(String subj, String pred, String obj) {
        addTriple(convertNonLiteral(subj), valueFactory.createURI(pred), convertNonLiteral(obj));
    }

    @Override
    public final void addPlainLiteral(String subj, String pred, String content, String lang) {
        if (lang == null) {
            addTriple(convertNonLiteral(subj), valueFactory.createURI(pred), valueFactory.createLiteral(content));
        } else {
            addTriple(convertNonLiteral(subj), valueFactory.createURI(pred),
                    valueFactory.createLiteral(content, lang));
        }
    }

    @Override
    public final void addTypedLiteral(String subj, String pred, String content, String type) {
        Literal literal = valueFactory.createLiteral(content, valueFactory.createURI(type));
        addTriple(convertNonLiteral(subj), valueFactory.createURI(pred), literal);
    }

    protected void addTriple(Resource subject, URI predicate, Value object) {
        try {
            handler.handleStatement(valueFactory.createStatement(subject, predicate, object));
        } catch(RDFHandlerException e) {
            // TODO: provide standard way to handle exceptions inside of triple sinks
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startStream() throws ParseException {
        try {
            handler.startRDF();
        } catch(RDFHandlerException e) {
            throw new ParseException(e);
        }
    }

    @Override
    public void endStream() throws ParseException {
        try {
            handler.endRDF();
        } catch(RDFHandlerException e) {
            throw new ParseException(e);
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
