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
package org.semarglproject.rdf.rdfa;

import org.semarglproject.rdf.core.ParseException;
import org.semarglproject.rdf.TurtleSerializer;
import org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.SaveToFileCallback;
import org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.TestCase;
import org.semarglproject.sink.CharOutputSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.test.TestNGHelper;
import org.semarglproject.vocab.rdfa.RDFa;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;

import static org.semarglproject.rdf.rdfa.RdfaTestSuiteHelper.runTestBundle;

public final class RdfaParserTest {

    private CharOutputSink charOutputSink;
    private StreamProcessor streamProcessor;
    private SaveToFileCallback semarglTurtleCallback = new SaveToFileCallback() {
        @Override
        public void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException {
            charOutputSink.connect(output);
            streamProcessor.setProperty(RdfaParser.RDFA_VERSION_PROPERTY, rdfaVersion);
            streamProcessor.setProperty(RdfaParser.ENABLE_VOCAB_EXPANSION, true);
            streamProcessor.process(input, inputUri);
        }

        @Override
        public String getOutputFileExt() {
            return "ttl";
        }
    };

    @BeforeClass
    public void init() throws SAXException, InterruptedException {
//        TestSuiteDownloadHelper.downloadAll(4);

        charOutputSink = new CharOutputSink("UTF-8");
        streamProcessor = new StreamProcessor(RdfaParser.connect(TurtleSerializer.connect(charOutputSink)));
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
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa10SvgTests(TestCase testCase) {
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_10);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa10XmlTests(TestCase testCase) {
        // TODO: investigate
        if (!testCase.name.contains("212")) {
            runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_10);
        }
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11Html4Tests(TestCase testCase) {
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11Xhtml1Tests(TestCase testCase) {
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11Html5Tests(TestCase testCase) {
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11XmlTests(TestCase testCase) {
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_11);
    }

    @Test(dataProvider = "getTestSuite")
    public void runRdfa11SvgTests(TestCase testCase) {
        runTestBundle(testCase, semarglTurtleCallback, RDFa.VERSION_11);
    }

}
