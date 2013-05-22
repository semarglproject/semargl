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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.test.SesameTestHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

// http://github.com/rdfa/rdfa-website/raw/master/manifest.ttl
public final class RdfaTestSuiteHelper {

    private static final String TEST_OUTPUT_DIR = "target/rdfa-output/";

    private static final String RDFA_TESTSUITE_ROOT = "http://rdfa.info/test-suite/test-cases/";
    private static final String RDFA_TESTSUITE_MANIFEST_URI = RDFA_TESTSUITE_ROOT + "manifest.ttl";

    private static final Map<String, String> LOCAL_MIRRORS = new HashMap<String, String>() {{
        put(RDFA_TESTSUITE_ROOT, "rdfa-testsuite/");
    }};

    private static final SesameTestHelper sth = new SesameTestHelper(TEST_OUTPUT_DIR, LOCAL_MIRRORS);
    private static final String FETCH_RDFA_TESTS_SPARQL = "http://rdfa.info/test-suite/test-cases/fetch_tests.sparql";

    // There are some errors in manifest.ttl
    private static String fixTestFilePath(String input, String pathDelta, String actualExt) {
        int pos = input.lastIndexOf('/');
        String path = input.substring(0, pos + 1);
        String filename = input.substring(pos + 1);
        return path + pathDelta + filename.replaceAll("\\.\\w+$", "." + actualExt);
    }

    public static Collection<TestCase> getTestSuite(String rdfaVersion, String docFormat) {
        String docExt = docFormat.replaceAll("[0-9]", "");
        String pathDelta = rdfaVersion + "/" + docFormat + "/";

        String queryStr = null;
        try {
            queryStr = IOUtils.toString(sth.openStreamForResource(FETCH_RDFA_TESTS_SPARQL));
        } catch (IOException e) {
            return null;
        }
        queryStr = queryStr.replace("!rdfa_version", "\"" + rdfaVersion + "\"").replace(
                "!host_lang", "\"" + docFormat + "\"");

        List<TestCase> testCases = sth.getTestCases(RDFA_TESTSUITE_MANIFEST_URI, queryStr, TestCase.class);
        for (TestCase testCase : testCases) {
            testCase.input = fixTestFilePath(testCase.input, pathDelta, docExt);
            testCase.result = fixTestFilePath(testCase.result, pathDelta, "sparql");
        }

        return testCases;
    }

    public static void runTestBundle(TestCase testCase, SaveToFileCallback callback, short rdfaVersion) {
        String resultFilePath = sth.getOutputPath(testCase.input, callback.getOutputFileExt());
        new File(resultFilePath).getParentFile().mkdirs();

        try {
            try {
                Reader input = new InputStreamReader(sth.openStreamForResource(testCase.input), "UTF-8");
                Writer output = new OutputStreamWriter(new FileOutputStream(resultFilePath), "UTF-8");
                try {
                    callback.run(input, testCase.input, output, rdfaVersion);
                } finally {
                    IOUtils.closeQuietly(input);
                    IOUtils.closeQuietly(output);
                }
            } catch (ParseException e) {
                // do nothing
            }

            String queryStr = IOUtils.toString(sth.openStreamForResource(testCase.result), "UTF-8");
            boolean expectedResult = testCase.expectedResult == null || Boolean.parseBoolean(testCase.expectedResult);
            assertEquals(sth.askModel(resultFilePath, queryStr, testCase.input), expectedResult);
        } catch (IOException e) {
            fail();
        }
    }

    public static void downloadAllTests(int parallelism) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        executorService.execute(new TestDownloadWorker("rdfa1.0", "xhtml1"));
        executorService.execute(new TestDownloadWorker("rdfa1.0", "svg"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "html4"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "xhtml1"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "html5"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "xml"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "svg"));
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
    }

    private static void downloadMissingTest(String testUrl) throws IOException {
        File inputFile = new File(testUrl.replace(RDFA_TESTSUITE_ROOT, "src/test/resources/rdfa-testsuite/"));
        if (!inputFile.exists()) {
            FileUtils.copyURLToFile(new URL(testUrl), inputFile);
        }
    }

    private static final class TestDownloadWorker implements Runnable {

        private Collection<TestCase> tests;

        private TestDownloadWorker(String rdfaVersion, String docFormat) {
            this.tests = getTestSuite(rdfaVersion, docFormat);
        }

        @Override
        public void run() {
            for (TestCase test : tests) {
                try {
                    downloadMissingTest(test.input);
                    downloadMissingTest(test.result);
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    public interface SaveToFileCallback {
        void run(Reader input, String inputUri, Writer output, short rdfaVersion) throws ParseException;
        String getOutputFileExt();
    }

    public final static class TestCase {
        public String name;
        public String input;
        public String result;
        public String expectedResult;

        @Override
        public String toString() {
            return name;
        }
    }

}
