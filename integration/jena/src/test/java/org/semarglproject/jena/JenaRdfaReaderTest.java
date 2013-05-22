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
package org.semarglproject.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.jena.rdf.rdfa.JenaRdfaReader;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper;
import org.semarglproject.vocab.RDFa;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.SaveToFileCallback;
import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.runTestBundle;

public class JenaRdfaReaderTest {

    private Model model;
    private SaveToFileCallback jenaCallback = new SaveToFileCallback() {
        @Override
        public void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException {
            try {
                model.read(input, inputUri, "RDFA");
            } finally {
                model.write(output, "TURTLE");
            }
        }

        @Override
        public String getOutputFileExt() {
            return "ttl";
        }
    };

    @BeforeClass
    public void init() throws SAXException, ClassNotFoundException {
        model = ModelFactory.createDefaultModel();
        JenaRdfaReader.inject();
    }

    @BeforeMethod
    public void setUp() {
        model.removeAll();
    }

    @DataProvider
    public static Object[][] getRdfa10Xhtml1TestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.0", "xhtml1"));
    }

    @DataProvider
    public static Object[][] getRdfa10SvgTestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.0", "svg"));
    }

    @DataProvider
    public static Object[][] getRdfa11Html4TestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "html4"));
    }

    @DataProvider
    public static Object[][] getRdfa11Xhtml1TestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "xhtml1"));
    }

    @DataProvider
    public static Object[][] getRdfa11Html5TestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "html5"));
    }

    @DataProvider
    public static Object[][] getRdfa11XmlTestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "xml"));
    }

    @DataProvider
    public static Object[][] getRdfa11SvgTestSuite() {
        return convertToDataProvider(RdfaTestSuiteHelper.getTestSuite("rdfa1.1", "svg"));
    }

    private static Object[][] convertToDataProvider(Collection<RdfaTestSuiteHelper.TestCase> tests) {
        Object[][] result = new Object[tests.size()][];
        int i = 0;
        for (RdfaTestSuiteHelper.TestCase testCase : tests) {
            result[i++] = new Object[]{testCase};
        }
        return result;
    }

    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
    public void runRdfa10Xhtml1Tests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getRdfa10SvgTestSuite")
    public void runRdfa10SvgTests(RdfaTestSuiteHelper.TestCase testCase) {
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getRdfa11Html4TestSuite")
    public void runRdfa11Html4Tests(RdfaTestSuiteHelper.TestCase testCase) {
        // vocabulary expansion is disabled by default
        if (testCase.input.matches(".+024[0-5].+")) {
            return;
        }
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
    public void runRdfa11Xhtml1Tests(RdfaTestSuiteHelper.TestCase testCase) {
        // vocabulary expansion is disabled by default
        if (testCase.input.matches(".+024[0-5].+")) {
            return;
        }
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11Html5TestSuite")
    public void runRdfa11Html5Tests(RdfaTestSuiteHelper.TestCase testCase) {
        // vocabulary expansion is disabled by default
        if (testCase.input.matches(".+024[0-5].+")) {
            return;
        }
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11XmlTestSuite")
    public void runRdfa11XmlTests(RdfaTestSuiteHelper.TestCase testCase) {
        // vocabulary expansion is disabled by default
        if (testCase.input.matches(".+024[0-5].+")) {
            return;
        }
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void runRdfa11SvgTests(RdfaTestSuiteHelper.TestCase testCase) {
        // vocabulary expansion is disabled by default
        if (testCase.input.matches(".+024[0-5].+")) {
            return;
        }
        runTestBundle(testCase, jenaCallback, RDFa.VERSION_11);
    }

}
