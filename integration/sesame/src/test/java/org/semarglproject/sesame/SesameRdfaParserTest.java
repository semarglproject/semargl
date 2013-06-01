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
package org.semarglproject.sesame;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper;
import org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.TestCase;
import org.semarglproject.sesame.core.sink.SesameSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.test.TestNGHelper;
import org.semarglproject.vocab.RDFa;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;

import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.SaveToFileCallback;
import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.runTestBundle;

public final class SesameRdfaParserTest {

    private StatementCollector model;
    private StreamProcessor streamProcessor;
    private SaveToFileCallback sesameCallback = new SaveToFileCallback() {
        @Override
        public void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException {
            streamProcessor.setProperty(RdfaParser.RDFA_VERSION_PROPERTY, rdfaVersion);
            try {
                streamProcessor.process(input, inputUri);
            } finally {
                RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, output);
                try {
                    rdfWriter.startRDF();
                    for(Statement nextStatement : model.getStatements()) {
                        rdfWriter.handleStatement(nextStatement);
                    }
                    rdfWriter.endRDF();
                } catch(RDFHandlerException e) {
                    // do nothing
                }
            }
        }

        @Override
        public String getOutputFileExt() {
            return "ttl";
        }
    };

    @BeforeClass
    public void init() throws SAXException {
        model = new StatementCollector();
        
        streamProcessor = new StreamProcessor(RdfaParser.connect(SesameSink.connect(model)));
        streamProcessor.setProperty(RdfaParser.ENABLE_VOCAB_EXPANSION, true);
    }

    @BeforeMethod
    public void setUp() {
        model.clear();
    }

    @DataProvider
    public static Object[][] getTestSuite(Method method) {
        String methodName = method.getName();
        String rdfaVersion = "rdfa1.0";
        if (methodName.startsWith("runRdfa11")) {
            rdfaVersion = "rdfa1.1";
        }
        String fileFormat = methodName.substring(9, methodName.indexOf("Tests")).toLowerCase();
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite(rdfaVersion, fileFormat));
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa10Xhtml1Tests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa10SvgTests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11Html4Tests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11Xhtml1Tests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11Html5Tests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11XmlTests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11SvgTests(TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

}
