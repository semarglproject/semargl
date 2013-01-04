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
package org.semarglproject.sesame.rdf.rdfa;

import org.openrdf.model.ValueFactory;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.ParseLocationListener;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.ProcessorGraphHandler;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.sesame.core.sink.SesameSink;
import org.semarglproject.vocab.RDFa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Implementation or Sesame's RDFParser on top of Semargl APIs.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 * @author Lev Khomich levkhomich@gmail.com
 *
 */
public final class SesameRDFaParser implements RDFParser, ProcessorGraphHandler {

    private boolean processorGraphEnabled;
    private boolean vocabExpansionEnabled;
    private boolean preserveBNodeIDs;
    private short rdfaCompatibility;

    private final StreamProcessor streamProcessor;

    private ParseErrorListener parseErrorListener;

    /**
     * Default constructor. Creates RDFa parser in 1.1 mode with disabled vocabulary expansion feature.
     * Properties can be changed using {@link #setParserConfig(org.openrdf.rio.ParserConfig)} method or
     * object's setters.
     */
    public SesameRDFaParser() {
        preserveBNodeIDs = true;
        vocabExpansionEnabled = false;
        processorGraphEnabled = false;
        rdfaCompatibility = RDFa.VERSION_11;
        parseErrorListener = null;
        streamProcessor = new StreamProcessor(RdfaParser.connect(SesameSink.connect(null)));
        streamProcessor.setProperty(RdfaParser.ENABLE_PROCESSOR_GRAPH, processorGraphEnabled);
        streamProcessor.setProperty(RdfaParser.ENABLE_VOCAB_EXPANSION, vocabExpansionEnabled);
        streamProcessor.setProperty(RdfaParser.PROCESSOR_GRAPH_HANDLER_PROPERTY, this);
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFaFormat.RDFA;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws RDFParseException, RDFHandlerException {
        InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
        try {
            parse(reader, baseURI);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    @Override
    public void parse(Reader reader, String baseURI) throws RDFParseException, RDFHandlerException {
        try {
            streamProcessor.process(reader, baseURI);
        } catch (ParseException e) {
            throw new RDFParseException(e);
        }
    }

    @Override
    public void setValueFactory(ValueFactory valueFactory) {
        streamProcessor.setProperty(SesameSink.VALUE_FACTORY_PROPERTY, valueFactory);
    }

    @Override
    public void setRDFHandler(RDFHandler handler) {
        streamProcessor.setProperty(SesameSink.RDF_HANDLER_PROPERTY, handler);
    }

    @Override
    public void setParseErrorListener(ParseErrorListener el) {
        this.parseErrorListener = el;
    }

    @Override
    public void setParseLocationListener(ParseLocationListener ll) {
        // not supported yet
    }

    @Override
    public void setParserConfig(ParserConfig config) {
        if (config instanceof RdfaParserConfig) {
            RdfaParserConfig rdfaParserConfig = (RdfaParserConfig) config;
            setProcessorGraphEnabled(rdfaParserConfig.isProcessorGraphEnabled());
            setVocabExpansionEnabled(rdfaParserConfig.isVocabExpansionEnabled());
            setRdfaCompatibility(rdfaParserConfig.getRdfaCompatibility());
        }
        this.preserveBNodeIDs = config.isPreserveBNodeIDs();
    }

    @Override
    public RdfaParserConfig getParserConfig() {
        return new RdfaParserConfig(false, true, preserveBNodeIDs, DatatypeHandling.IGNORE,
                processorGraphEnabled, vocabExpansionEnabled, rdfaCompatibility);
    }

    @Override
    public void setVerifyData(boolean verifyData) {
        // ignore
    }

    @Override
    public void setPreserveBNodeIDs(boolean preserveBNodeIDs) {
        this.preserveBNodeIDs = preserveBNodeIDs;
    }

    @Override
    public void setStopAtFirstError(boolean stopAtFirstError) {
        if (!stopAtFirstError) {
            throw new IllegalArgumentException("Parser doesn't support stopAtFirstError = " + stopAtFirstError);
        }
    }

    @Override
    public void setDatatypeHandling(DatatypeHandling datatypeHandling) {
        if (!datatypeHandling.equals(DatatypeHandling.IGNORE)) {
            throw new IllegalArgumentException("Parser doesn't support datatypeHandling = " + datatypeHandling.name());
        }
    }

    /**
     * Changes {@link RdfaParser#ENABLE_PROCESSOR_GRAPH} setting
     * @param processorGraphEnabled new value to be set
     */
    public void setProcessorGraphEnabled(boolean processorGraphEnabled) {
        this.processorGraphEnabled = processorGraphEnabled;
        streamProcessor.setProperty(RdfaParser.ENABLE_PROCESSOR_GRAPH, processorGraphEnabled);
    }

    /**
     * Changes {@link RdfaParser#ENABLE_VOCAB_EXPANSION} setting
     * @param vocabExpansionEnabled new value to be set
     */
    public void setVocabExpansionEnabled(boolean vocabExpansionEnabled) {
        this.vocabExpansionEnabled = vocabExpansionEnabled;
        streamProcessor.setProperty(RdfaParser.ENABLE_VOCAB_EXPANSION, vocabExpansionEnabled);
    }

    /**
     * Changes {@link RdfaParser#RDFA_VERSION_PROPERTY} setting
     * @param rdfaCompatibility new value to be set
     */
    public void setRdfaCompatibility(short rdfaCompatibility) {
        this.rdfaCompatibility = rdfaCompatibility;
        streamProcessor.setProperty(RdfaParser.RDFA_VERSION_PROPERTY, rdfaCompatibility);
    }

    @Override
    public void info(String infoClass, String message) {
    }

    @Override
    public void warning(String warningClass, String message) {
        if (parseErrorListener != null) {
            parseErrorListener.warning(message, -1, -1);
        }
    }

    @Override
    public void error(String errorClass, String message) {
        if (parseErrorListener != null) {
            parseErrorListener.error(message, -1, -1);
        }
    }
}
