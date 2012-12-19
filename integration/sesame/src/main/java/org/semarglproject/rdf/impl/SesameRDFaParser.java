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

import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFParserBase;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.rdfa.RdfaParser;

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
public class SesameRDFaParser extends RDFParserBase implements RDFParser {

    private DataProcessor<Reader> dp;
    private RdfaParser rdfaParser = new RdfaParser(true, true, true);

    public SesameRDFaParser() {
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
        rdfaParser.streamingTo(new SesameTripleSink(ValueFactoryImpl.getInstance(), this.getRDFHandler()));
        try {
            dp.process(reader, baseURI);
        } catch (ParseException e) {
            throw new RDFParseException(e);
        }
    }
}
