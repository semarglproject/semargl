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

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.rdf.rdfa.RdfaTestBundle;
import org.semarglproject.rdf.rdfa.RdfaTestBundle.SaveToFileCallback;
import org.semarglproject.rdf.rdfa.RdfaTestBundle.TestCase;
import org.semarglproject.vocab.RDFa;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import static org.semarglproject.rdf.rdfa.RdfaTestBundle.runTestBundle;

public final class RdfaParserTest {

    private MGraph graph;
    private DataProcessor<Reader> dp;
    private RdfaParser rdfaParser;
    private SaveToFileCallback clerezzaCallback = new SaveToFileCallback() {
        @Override
        public void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException {
            rdfaParser.setRdfaVersion(rdfaVersion);
            try {
                dp.process(input, inputUri);
            } finally {
                OutputStream outputStream = new WriterOutputStream(output, "UTF-8");
                try {
                    Serializer serializer = Serializer.getInstance();
                    serializer.serialize(outputStream, graph, "text/turtle");
                } finally {
                    IOUtils.closeQuietly(outputStream);
                }
            }
        }
    };

    @BeforeClass
    public void init() throws SAXException {
        RdfaTestBundle.prepareTestDir();

        UriRef graphUri = new UriRef("http://example.com/");
        TcManager MANAGER = TcManager.getInstance();
        if (MANAGER.listMGraphs().contains(graphUri)) {
            MANAGER.deleteTripleCollection(graphUri);
        }
        graph = MANAGER.createMGraph(graphUri);

        rdfaParser = new RdfaParser(true, true, true);
        dp = new SaxSource().streamingTo(
                rdfaParser.streamingTo(
                        new ClerezzaTripleSink(graph))).build();
    }

    @BeforeMethod
    public void setUp() {
        if (graph != null) {
            graph.clear();
        }
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

    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
    public void Rdfa10Xhtml1TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getRdfa10SvgTestSuite")
    public void Rdfa10SvgTestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getRdfa11Html4TestSuite")
    public void Rdfa11Html4TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
    public void Rdfa11Xhtml1TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11Html5TestSuite")
    public void Rdfa11Html5TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11XmlTestSuite")
    public void Rdfa11XmlTestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void Rdfa11SvgTestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaCallback, RDFa.VERSION_11);
    }

}
