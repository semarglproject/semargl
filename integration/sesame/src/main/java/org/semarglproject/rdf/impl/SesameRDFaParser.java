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

import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.ParseErrorListener;
import org.openrdf.rio.ParseLocationListener;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.vocab.RDFa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public class SesameRDFaParser implements RDFParser {

    private boolean processorGraphEnabled;
    private boolean vocabExpansionEnabled;
    private boolean preserveBNodeIDs;
    private short rdfaCompatibility;

    private DataProcessor<Reader> dp;
    private final RdfaParser rdfaParser;

    private RDFHandler rdfHandler;
    private ValueFactory valueFactory;

    public SesameRDFaParser() {
        preserveBNodeIDs = true;
        vocabExpansionEnabled = false;
        processorGraphEnabled = false;
        rdfaCompatibility = RDFa.VERSION_11;
        rdfHandler = null;
        valueFactory = null;
        rdfaParser = new RdfaParser(true, processorGraphEnabled, vocabExpansionEnabled);
        dp = new SaxSource().streamingTo(rdfaParser).build();
    }

    @Override
    public RDFFormat getRDFFormat() {
        return RDFaFormat.RDFA;
    }

    @Override
    public void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
        this.parse(new InputStreamReader(in, Charset.forName("UTF-8")), baseURI);
    }

    @Override
    public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
        if (valueFactory == null) {
            rdfaParser.streamingTo(new SesameTripleSink(ValueFactoryImpl.getInstance(), rdfHandler));
        } else {
            rdfaParser.streamingTo(new SesameTripleSink(valueFactory, rdfHandler));
        }
        try {
            dp.process(reader, baseURI);
        } catch (ParseException e) {
            throw new RDFParseException(e);
        }
    }

    @Override
    public void setValueFactory(ValueFactory valueFactory) {
        this.valueFactory = valueFactory;
    }

    @Override
    public void setRDFHandler(RDFHandler handler) {
        this.rdfHandler = handler;
    }

    @Override
    public void setParseErrorListener(ParseErrorListener el) {
        // not supported yet
    }

    @Override
    public void setParseLocationListener(ParseLocationListener ll) {
        // not supported yet
    }

    @Override
    public void setParserConfig(ParserConfig config) {
        if (config instanceof RdfaParserConfig) {
            RdfaParserConfig rdfaParserConfig = (RdfaParserConfig) config;
            this.processorGraphEnabled = rdfaParserConfig.isProcessorGraphEnabled();
            this.vocabExpansionEnabled = rdfaParserConfig.isVocabExpansionEnabled();
            this.rdfaCompatibility = rdfaParserConfig.getRdfaCompatibility();
        }
        this.preserveBNodeIDs = config.isPreserveBNodeIDs();

        rdfaParser.setOutput(true, processorGraphEnabled, vocabExpansionEnabled);
        rdfaParser.setRdfaVersion(rdfaCompatibility);
    }

    @Override
    public RdfaParserConfig getParserConfig() {
        return new RdfaParserConfig(preserveBNodeIDs, processorGraphEnabled, vocabExpansionEnabled, rdfaCompatibility);
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

    public void setProcessorGraphEnabled(boolean processorGraphEnabled) {
        this.processorGraphEnabled = processorGraphEnabled;
        rdfaParser.setOutput(true, processorGraphEnabled, vocabExpansionEnabled);
    }

    public void setVocabExpansionEnabled(boolean vocabExpansionEnabled) {
        this.vocabExpansionEnabled = vocabExpansionEnabled;
        rdfaParser.setOutput(true, processorGraphEnabled, vocabExpansionEnabled);
    }

    public void setRdfaCompatibility(short rdfaCompatibility) {
        this.rdfaCompatibility = rdfaCompatibility;
        rdfaParser.setRdfaVersion(rdfaCompatibility);
    }
}
