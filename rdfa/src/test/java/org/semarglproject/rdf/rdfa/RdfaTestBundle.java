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

package org.semarglproject.rdf.rdfa;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semarglproject.rdf.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

// http://github.com/rdfa/rdfa-website/raw/master/manifest.ttl
public final class RdfaTestBundle {

    private static final String FAILURES_DIR = "target/failed/";

    private static final String RDFA_TESTSUITE_ROOT = "http://rdfa.info/test-suite/test-cases/";
    private static final String RDFA_TESTSUITE_MANIFEST_URI = RDFA_TESTSUITE_ROOT + "manifest.ttl";

    public interface SaveToFileCallback {
        void run(Reader input, String inputUri, Writer output) throws ParseException;
    }

    public final static class TestCase {
        private final String name;
        private final String input;
        private final String result;
        private final boolean expectedResult;

        public TestCase(String name, String input, String result, boolean expectedResult) {
            super();
            this.name = name;
            this.input = input;
            this.result = result;
            this.expectedResult = expectedResult;
        }

        public String getInput() {
            return input;
        }

        public String getResult() {
            return result;
        }

        public boolean getExpectedResult() {
            return expectedResult;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // There are some errors in manifest.ttl
    private static String fixTestFilePath(String input, String pathDelta, String actualExt) {
        int pos = input.lastIndexOf('/');
        String path = input.substring(0, pos + 1);
        String filename = input.substring(pos + 1);
        return path + pathDelta + filename.replaceAll("\\.\\w+$", "." + actualExt);
    }

    public static Collection<TestCase> getTestCases(String rdfaVersion, String docFormat) {
        Collection<TestCase> testCases = new ArrayList<TestCase>();
        String docExt = docFormat.replaceAll("[0-9]", "");

        String queryStr = null;
        Model graph = ModelFactory.createDefaultModel();
        try {
            graph.read(openStreamForResource(RDFA_TESTSUITE_MANIFEST_URI), RDFA_TESTSUITE_MANIFEST_URI, "TTL");
            queryStr = IOUtils.toString(openStreamForResource("rdfa-testsuite/fetch_tests.sparql"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        queryStr = queryStr.replace("!rdfa_version", "\"" + rdfaVersion + "\"").replace(
                "!host_lang", "\"" + docFormat + "\"");

        Query query = QueryFactory.create(queryStr, RDFA_TESTSUITE_MANIFEST_URI);
        QueryExecution qe = QueryExecutionFactory.create(query, graph);
        ResultSet rs = qe.execSelect();
        Set<String> addedCases = new HashSet<String>();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String caseName = qs.getResource("test_case").getURI();

            // skip declaration duplicates in manifest
            if (addedCases.contains(caseName)) {
                continue;
            }
            addedCases.add(caseName);

            boolean expectedResult = true;
            if (qs.getLiteral("exp_result") != null) {
                expectedResult = qs.getLiteral("exp_result").getBoolean();
                // String descr = qs.getLiteral("descr").getString();
            }

            String pathDelta = rdfaVersion + "/" + docFormat + "/";
            String input = qs.getResource("input").getURI();
            input = fixTestFilePath(input, pathDelta, docExt);

            String result = qs.getResource("result").getURI();
            result = fixTestFilePath(result, pathDelta, "sparql");

            try {
                int testNum = Integer.parseInt(caseName.substring(caseName.lastIndexOf("/") + 1));
                // there is no way to detect rdfa version from that document
                if (testNum == 294 && rdfaVersion.equals("rdfa1.0") && docFormat.equals("svg")) {
                    continue;
                }
            } catch (NumberFormatException e) {
                // no problems here
            }

            testCases.add(new TestCase(caseName, input, result, expectedResult));
        }
        qe.close();
        return testCases;
    }

    public static void prepareTestDir() {
        try {
            File testDir = new File(FAILURES_DIR);
            testDir.mkdirs();
            FileUtils.cleanDirectory(testDir);
        } catch (IOException e) {
            // do nothing
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

    public static void runTestBundle(TestCase testCase, SaveToFileCallback callback) {
        String inputUri = testCase.getInput();

        String resultFilePath = getOutputPath(inputUri);
        new File(resultFilePath).getParentFile().mkdirs();

        try {
            try {
                Reader input = new InputStreamReader(openStreamForResource(inputUri));
                Writer output = new FileWriter(resultFilePath);
                try {
                    callback.run(input, inputUri, output);
                } finally {
                    IOUtils.closeQuietly(input);
                    IOUtils.closeQuietly(output);
                }
            } catch (ParseException e) {
                // do nothing
            }

            Model resultModel = createModelFromFile(resultFilePath, inputUri);
            String queryStr = IOUtils.toString(openStreamForResource(testCase.getResult()));
            assertEquals(askModel(resultModel, queryStr), testCase.getExpectedResult());
        } catch (IOException e) {
            fail();
        }
    }

    private static void downloadMissingTest(String testUrl) throws IOException {
        File inputFile = new File(testUrl.replace(RDFA_TESTSUITE_ROOT, "src/test/resources/rdfa-testsuite/"));
        if (!inputFile.exists()) {
            FileUtils.copyURLToFile(new URL(testUrl), inputFile);
        }
    }

    private static boolean askModel(Model model, String queryStr) throws IOException {
        Query query = QueryFactory.create(queryStr);
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        boolean result = qe.execAsk();
        qe.close();
        return result;
    }

    private static String getOutputPath(String uri) {
        if (uri == null) {
            return null;
        }
        String result = uri;
        if (uri.startsWith(RDFA_TESTSUITE_ROOT)) {
            result = uri.replace(RDFA_TESTSUITE_ROOT, FAILURES_DIR);
        }
        return result + ".out.ttl";
    }

    private static Model createModelFromFile(String filename, String baseUri) throws FileNotFoundException {
        String fileFormat = detectFileFormat(filename);
        Model result = ModelFactory.createDefaultModel();
        InputStream inputStream = openStreamForResource(filename);
        try {
            result.read(inputStream, baseUri, fileFormat);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    private static String detectFileFormat(String filename) {
        String fileFormat;
        if (filename.endsWith(".nt")) {
            fileFormat = "N-TRIPLE";
        } else if (filename.endsWith(".ttl")) {
            fileFormat = "TURTLE";
        } else if (filename.endsWith(".rdf")) {
            fileFormat = "RDF/XML";
        } else {
            throw new IllegalArgumentException("Unknown file format");
        }
        return fileFormat;
    }

    private static InputStream openStreamForResource(String uri) throws FileNotFoundException {
        String result = uri;
        if (uri.startsWith(RDFA_TESTSUITE_ROOT)) {
            result = uri.replace(RDFA_TESTSUITE_ROOT, "rdfa-testsuite/");
        }
        File file = new File(uri);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        result = RdfaTestBundle.class.getClassLoader().getResource(result).getFile();
        if (result.contains(".jar!/")) {
            try {
                return new URL("jar:" + result).openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return new FileInputStream(result);
    }

    private static final class TestDownloadWorker implements Runnable {

        private final Collection<TestCase> tests;

        private TestDownloadWorker(String rdfaVersion, String docFormat) {
            this.tests = getTestCases(rdfaVersion, docFormat);
        }

        @Override
        public void run() {
            for (TestCase test : tests) {
                try {
                    downloadMissingTest(test.getInput());
                    downloadMissingTest(test.getResult());
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

}
