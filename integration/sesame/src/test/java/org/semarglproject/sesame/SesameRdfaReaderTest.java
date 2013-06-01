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

import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.rio.helpers.RDFaParserSettings;
import org.openrdf.rio.helpers.RDFaVersion;
import org.openrdf.rio.helpers.StatementCollector;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper;
import org.semarglproject.sesame.rdf.rdfa.SemarglParserSettings;
import org.semarglproject.test.TestNGHelper;
import org.semarglproject.vocab.RDFa;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.SaveToFileCallback;
import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.runTestBundle;

public class SesameRdfaReaderTest {

    private Model model;
    private SaveToFileCallback sesameCallback = new SaveToFileCallback() {
        @Override
        public void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException {
            try {
                RDFParser rdfParser = Rio.createParser(RDFFormat.RDFA);
                ParserConfig parserConfig = rdfParser.getParserConfig();
                parserConfig.set(RDFaParserSettings.RDFA_COMPATIBILITY, RDFaVersion.RDFA_1_1);
                parserConfig.set(RDFaParserSettings.VOCAB_EXPANSION_ENABLED, true);
                parserConfig.set(SemarglParserSettings.PROCESSOR_GRAPH_ENABLED, true);
                parserConfig.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
                parserConfig.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
                parserConfig.addNonFatalError(BasicParserSettings.VERIFY_RELATIVE_URIS);
                rdfParser.setRDFHandler(new StatementCollector(model));
                rdfParser.parse(input, inputUri);
            } catch (OpenRDFException e) {
                // do nothing
                // FIXME: Why not fail quickly here?
            } catch (IOException e) {
                // do nothing
            } finally {
                RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, output);
                try {
                    rdfWriter.startRDF();
                    for(Statement nextStatement : model) {
                        rdfWriter.handleStatement(nextStatement);
                    }
                    rdfWriter.endRDF();
                } catch (RDFHandlerException e) {
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
    public void init() throws SAXException, ClassNotFoundException {
        model = new LinkedHashModel();
    }

    @BeforeMethod
    public void setUp() {
        model.clear();
    }

    @DataProvider
    public static Object[][] getRdfa10Xhtml1TestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.0", "xhtml1"));
    }

    @DataProvider
    public static Object[][] getRdfa10SvgTestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.0", "svg"));
    }

    @DataProvider
    public static Object[][] getRdfa11Html4TestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "html4"));
    }

    @DataProvider
    public static Object[][] getRdfa11Xhtml1TestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "xhtml1"));
    }

    @DataProvider
    public static Object[][] getRdfa11Html5TestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "html5"));
    }

    @DataProvider
    public static Object[][] getRdfa11XmlTestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "xml"));
    }

    @DataProvider
    public static Object[][] getRdfa11SvgTestSuite() {
        return TestNGHelper.toArray(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "svg"));
    }

    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
    public void runRdfa10Xhtml1Tests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getRdfa10SvgTestSuite")
    public void runRdfa10SvgTests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getRdfa11Html4TestSuite")
    public void runRdfa11Html4Tests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
    public void runRdfa11Xhtml1Tests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11Html5TestSuite")
    public void runRdfa11Html5Tests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11XmlTestSuite")
    public void runRdfa11XmlTests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void runRdfa11SvgTests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, sesameCallback, RDFa.VERSION_11);
    }

}
