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

package org.semarglproject.jsonld;

import org.apache.commons.io.IOUtils;
import org.semarglproject.rdf.NQuadsSerializer;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.sink.CharOutputSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.test.SesameTestHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

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

public class JsonLdParserTest {

    private static final String TEST_OUTPUT_DIR = "target/json-output/";
    public static final String FETCH_TESTS_SPARQL = "fetch_tests.sparql";

    private static final Map<String, String> LOCAL_MIRRORS = new HashMap<String, String>() {{
        put("http://json-ld.org/test-suite/tests/", "json-ld-org/");
    }};

    private static final String TESTSUITE_MANIFEST_URI = "http://json-ld.org/test-suite/tests/Manifest.ttl";

    private CharOutputSink charOutputSink;
    private StreamProcessor streamProcessor;
    private SesameTestHelper sth;

    @BeforeClass
    public void init() throws IOException, SAXException {
        sth = new SesameTestHelper(TEST_OUTPUT_DIR, LOCAL_MIRRORS);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        charOutputSink = new CharOutputSink();
        charOutputSink.connect(System.out);

        streamProcessor = new StreamProcessor(JsonLdParser.connect(NQuadsSerializer.connect(charOutputSink)));
    }

    @DataProvider
    public Object[][] getTestSuite() {
        String queryStr;
        try {
            queryStr = IOUtils.toString(sth.openStreamForResource(FETCH_TESTS_SPARQL));
        } catch (IOException e) {
            return null;
        }
        List<TestCase> testCases = sth.getTestCases(TESTSUITE_MANIFEST_URI, queryStr, TestCase.class);
        Object[][] result = new Object[testCases.size()][];
        for (int i = 0; i < testCases.size(); i++) {
            result[i] = new Object[] { testCases.get(i) };
        }
        return result;
    }

    @Test(dataProvider = "getTestSuite")
    public void runWithNQuadsSink(TestCase testCase) {
        runTest(testCase, new SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                charOutputSink.connect(output);
                streamProcessor.process(input, inputUri);
            }
        });
    }

    public void runTest(TestCase testCase, SaveToFileCallback callback) {
        String resultFilePath = sth.getOutputPath(testCase.input, "nq");
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
        } catch (IOException e) {
            fail();
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }
        assertTrue(sth.areModelsEqual(resultFilePath, testCase.result, testCase.input));
    }

    public interface SaveToFileCallback {
        void run(Reader input, String inputUri, Writer output) throws ParseException;
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
