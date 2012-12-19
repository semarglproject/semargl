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

import org.openrdf.model.Statement;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.rdf.rdfa.RdfaTestBundle;
import org.semarglproject.rdf.rdfa.RdfaTestBundle.TestCase;
import org.semarglproject.vocab.RDFa;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import static org.semarglproject.rdf.rdfa.RdfaTestBundle.SaveToFileCallback;
import static org.semarglproject.rdf.rdfa.RdfaTestBundle.runTestBundle;

public final class RdfaParserTest {

    private StatementCollector model;
    private RdfaParser rdfaParser;
    private DataProcessor<Reader> dp;
    private SaveToFileCallback sesameCallback = new SaveToFileCallback() {
        @Override
        public void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException {
            rdfaParser.setRdfaVersion(rdfaVersion);
            try {
                dp.process(input, inputUri);
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
    };

    @BeforeClass
    public void init() throws SAXException {
        RdfaTestBundle.prepareTestDir();
        model = new StatementCollector();
        
        rdfaParser = new RdfaParser(true, true, true);
        dp = new SaxSource().streamingTo(
                rdfaParser.streamingTo(
                        new SesameTripleSink(ValueFactoryImpl.getInstance(), model))).build();
    }

    @BeforeMethod
    public void setUp() {
        model.clear();
    }

    @DataProvider
    public static Object[][] getRdfa10Xhtml1TestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.0", "xhtml1"));
    }

    @DataProvider
    public static Object[][] getRdfa10SvgTestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.0", "svg"));
    }

    @DataProvider
    public static Object[][] getRdfa11Html4TestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.1", "html4"));
    }

    @DataProvider
    public static Object[][] getRdfa11Xhtml1TestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.1", "xhtml1"));
    }

    @DataProvider
    public static Object[][] getRdfa11Html5TestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.1", "html5"));
    }

    @DataProvider
    public static Object[][] getRdfa11XmlTestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.1", "xml"));
    }

    @DataProvider
    public static Object[][] getRdfa11SvgTestSuite() {
        return convertToDataProvider(RdfaTestBundle.getTestCases("rdfa1.1", "svg"));
    }

    private static Object[][] convertToDataProvider(Collection<TestCase> tests) {
        Object[][] result = new Object[tests.size()][];
        int i = 0;
        for (TestCase testCase : tests) {
            result[i++] = new Object[]{testCase};
        }
        return result;
    }

//    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
//    public void Rdfa10Xhtml1TestsSesame(TestCase testCase) {
//        runTestBundle(testCase, sesameCallback, RDFa.VERSION_10);
//    }
//
//    @Test(dataProvider = "getRdfa10SvgTestSuite")
//    public void Rdfa10SvgTestsSesame(TestCase testCase) {
//        runTestBundle(testCase, sesameCallback, RDFa.VERSION_10);
//    }
//
//    @Test(dataProvider = "getRdfa11Html4TestSuite")
//    public void Rdfa11Html4TestsSesame(TestCase testCase) {
//        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
//    }
//
//    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
//    public void Rdfa11Xhtml1TestsSesame(TestCase testCase) {
//        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
//    }
//
//    @Test(dataProvider = "getRdfa11Html5TestSuite")
//    public void Rdfa11Html5TestsSesame(TestCase testCase) {
//        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
//    }
//
//    @Test(dataProvider = "getRdfa11XmlTestSuite")
//    public void Rdfa11XmlTestsSesame(TestCase testCase) {
//        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
//    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void Rdfa11SvgTestsSesame(TestCase testCase) {
        if (testCase.getInput().contains("0236"))
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

}
