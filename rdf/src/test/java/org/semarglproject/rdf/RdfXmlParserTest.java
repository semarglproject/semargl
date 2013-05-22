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
package org.semarglproject.rdf;

import org.apache.commons.io.IOUtils;
import org.semarglproject.sink.CharOutputSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.test.SesameTestHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public final class RdfXmlParserTest {

    private static final String TEST_OUTPUT_DIR = "target/rdfxml-output/";
    private static final String ARP_TESTSUITE_ROOT = "http://jcarroll.hpl.hp.com/arp-tests/";
    private static final String W3C_TESTSUITE_ROOT = "http://www.w3.org/2000/10/rdf-tests/rdfcore/";
    public static final String FETCH_RDFXML_TESTS_SPARQL = "fetch_rdfxml_tests.sparql";

    private static final Map<String, String> LOCAL_MIRRORS = new HashMap<String, String>() {{
        put(W3C_TESTSUITE_ROOT, "w3c/");
        put(ARP_TESTSUITE_ROOT, "arp/");
    }};

    private static final String ARP_TESTSUITE_MANIFEST_URI = ARP_TESTSUITE_ROOT + "Manifest.rdf";
    private static final String W3C_TESTSUITE_MANIFEST_URI = W3C_TESTSUITE_ROOT + "Manifest.rdf";

    private CharOutputSink charOutputSink;
    private StreamProcessor streamProcessorTtl;
    private StreamProcessor streamProcessorNt;
    private SesameTestHelper sth;

    @BeforeClass
    public void init() {
        sth = new SesameTestHelper(TEST_OUTPUT_DIR, LOCAL_MIRRORS);
        charOutputSink = new CharOutputSink("UTF-8");
        streamProcessorTtl = new StreamProcessor(RdfXmlParser.connect(TurtleSerializer.connect(charOutputSink)));
        streamProcessorNt = new StreamProcessor(RdfXmlParser.connect(NTriplesSerializer.connect(charOutputSink)));
    }

    @DataProvider
    public Object[][] getTestSuite() throws IOException {
        String queryStr = IOUtils.toString(sth.openStreamForResource(FETCH_RDFXML_TESTS_SPARQL));
        List<TestCase> testCases = sth.getTestCases(ARP_TESTSUITE_MANIFEST_URI, queryStr, TestCase.class);
        testCases.addAll(sth.getTestCases(W3C_TESTSUITE_MANIFEST_URI, queryStr, TestCase.class));

        Object[][] result = new Object[testCases.size()][];
        for (int i = 0; i < testCases.size(); i++) {
            result[i] = new Object[] { testCases.get(i) };
        }
        return result;
    }

    @Test(dataProvider = "getTestSuite")
    public void runWithTurtleSink(TestCase testCase) {
        runTest(testCase, new SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                charOutputSink.connect(output);
                streamProcessorTtl.process(input, inputUri);
            }

            @Override
            public String getOutputFileExt() {
                return "ttl";
            }
        });
    }

    @Test(dataProvider = "getTestSuite")
    public void runWithNTriplesSink(TestCase testCase) {
        runTest(testCase, new SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                charOutputSink.connect(output);
                streamProcessorNt.process(input, inputUri);
            }

            @Override
            public String getOutputFileExt() {
                return "nt";
            }
        });
    }

    public void runTest(TestCase testCase, SaveToFileCallback callback) {
        String resultFilePath = sth.getOutputPath(testCase.input, callback.getOutputFileExt());
        new File(resultFilePath).getParentFile().mkdirs();
        try {
            Reader input = new InputStreamReader(sth.openStreamForResource(testCase.input), "UTF-8");
            Writer output = new OutputStreamWriter(new FileOutputStream(resultFilePath), "UTF-8");
            try {
                callback.run(input, testCase.input, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        } catch (ParseException e) {
            resultFilePath = null;
        } catch (IOException e) {
            fail();
        }
        assertTrue(sth.areModelsEqual(resultFilePath, testCase.result, testCase.input));
    }

    public interface SaveToFileCallback {
        void run(Reader input, String inputUri, Writer output) throws ParseException;
        String getOutputFileExt();
    }

    public final static class TestCase {
        public String name;
        public String input;
        public String result;

        @Override
        public String toString() {
            return name;
        }
    }

}
