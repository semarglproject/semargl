/**
 * Copyright 2012-2013 Lev Khomich
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public final class RdfXmlTestBundle {

    private static final String FAILURES_DIR = "target/failed/";

    private static final String ARP_TESTSUITE_ROOT = "http://jcarroll.hpl.hp.com/arp-tests/";
    private static final String ARP_TESTSUITE_MANIFEST_URI = ARP_TESTSUITE_ROOT + "Manifest.rdf";

    private static final String W3C_TESTSUITE_ROOT = "http://www.w3.org/2000/10/rdf-tests/rdfcore/";
    private static final String W3C_TESTSUITE_MANIFEST_URI = W3C_TESTSUITE_ROOT + "Manifest.rdf";

    public static final String FETCH_RDFXML_TESTS_SPARQL = "fetch_rdfxml_tests.sparql";

    public interface SaveToFileCallback {
        String run(Reader input, String inputUri, Writer output) throws ParseException;
    }

    public final static class TestCase {
        private final String name;
        private final String input;
        private final String result;

        public TestCase(String name, String input, String result) {
            super();
            this.name = name;
            this.input = input;
            this.result = result;
        }

        public String getInput() {
            return input;
        }

        public String getResult() {
            return result;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Object[][] getTestFiles() {
        List<TestCase> testCases = getTestCases(W3C_TESTSUITE_MANIFEST_URI, W3C_TESTSUITE_ROOT);
        testCases.addAll(getTestCases(ARP_TESTSUITE_MANIFEST_URI, ARP_TESTSUITE_ROOT));
        Object[][] result = new Object[testCases.size()][];
        for (int i = 0; i < testCases.size(); i++) {
            result[i] = new Object[] { testCases.get(i) };
        }
        return result;
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

    private static List<TestCase> getTestCases(String manifestUri, String testsuiteRoot) {
        List<TestCase> testCases = new ArrayList<TestCase>();
        String queryStr = null;
        Model graph = ModelFactory.createDefaultModel();
        try {
            graph.read(openStreamForResource(manifestUri), manifestUri, "RDF/XML");
            queryStr = IOUtils.toString(openStreamForResource(FETCH_RDFXML_TESTS_SPARQL));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Query query = QueryFactory.create(queryStr, manifestUri);
        QueryExecution qe = QueryExecutionFactory.create(query, graph);
        ResultSet rs = qe.execSelect();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String testName = qs.getResource("test_case").getURI().replace(testsuiteRoot, "");
            String input = qs.getResource("input").getURI();

            String result = null;
            if (qs.getResource("result") != null) {
                result = qs.getResource("result").getURI();
            }

            // parser produces valid output, but tests fail because
            // XMLLiterals can't be compared like plain strings
            if (input.endsWith("rdfms-xml-literal-namespaces/test002.rdf")
                    || input.endsWith("xml-literals/html.rdf")
                    || input.endsWith("xml-literals/reported1.rdf")
                    || input.endsWith("xml-literals/reported2.rdf")
                    || input.endsWith("xml-literals/reported3.rdf")

                    // validating Jena parser crashes here
                    || input.endsWith("i18n/t9000.rdf")) {
                continue;
            }
            testCases.add(new TestCase(testName, input, result));
        }
        qe.close();
        return testCases;
    }

    public static void runTestWith(TestCase testCase, SaveToFileCallback callback) {
        String inputUri = testCase.getInput();

        String resultFilePath = getOutputPath(inputUri);
        new File(resultFilePath).getParentFile().mkdirs();

        String fileExt = null;
        boolean invalidRdfXmlFile = false;
        try {
            Reader input = new InputStreamReader(openStreamForResource(inputUri), "UTF-8");
            Writer output = new OutputStreamWriter(new FileOutputStream(resultFilePath), "UTF-8");
            try {
                fileExt = callback.run(input, inputUri, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        } catch (IOException e) {
            fail();
        } catch (ParseException e) {
            invalidRdfXmlFile = true;
        }
        try {
            Model inputModel;
            if (invalidRdfXmlFile) {
                // in case of invalid input, we should ignore all document
                inputModel = ModelFactory.createDefaultModel();
            } else {
                inputModel = createModelFromFile(resultFilePath, inputUri, fileExt);
            }
            Model expected;
            if (testCase.getResult() == null) {
                // negative test cases assume no resulting triples
                expected = ModelFactory.createDefaultModel();
            } else {
                expected = createModelFromFile(testCase.getResult(), inputUri, testCase.getResult());
            }
            assertTrue(inputModel.isIsomorphicWith(expected));
        } catch (FileNotFoundException e) {
            fail();
        }
    }

    private static String getOutputPath(String uri) {
        String result = uri;
        if (uri.startsWith(W3C_TESTSUITE_ROOT)) {
            result = uri.replace(W3C_TESTSUITE_ROOT, FAILURES_DIR + "w3c/");
        } else if (uri.startsWith(ARP_TESTSUITE_ROOT)) {
            result = uri.replace(ARP_TESTSUITE_ROOT, FAILURES_DIR + "arp/");
        }
        return result + ".out";
    }

    private static Model createModelFromFile(String filename, String baseUri,
                                             String fileExt) throws FileNotFoundException {
        String fileFormat = detectFileFormat(fileExt);
        Model result = ModelFactory.createDefaultModel();
        InputStream inputStream = openStreamForResource(filename);
        try {
            result.read(inputStream, baseUri, fileFormat);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }

    private static String detectFileFormat(String fileExt) {
        String fileFormat;
        if (fileExt.endsWith(".nt")) {
            fileFormat = "N-TRIPLE";
        } else if (fileExt.endsWith(".ttl")) {
            fileFormat = "TURTLE";
        } else if (fileExt.endsWith(".rdf")) {
            fileFormat = "RDF/XML";
        } else {
            throw new IllegalArgumentException("Unknown file format");
        }
        return fileFormat;
    }

    private static InputStream openStreamForResource(String uri) throws FileNotFoundException {
        String result = uri;
        if (uri.startsWith(W3C_TESTSUITE_ROOT)) {
            result = uri.replace(W3C_TESTSUITE_ROOT, "w3c/");
        } else if (uri.startsWith(ARP_TESTSUITE_ROOT)) {
            result = uri.replace(ARP_TESTSUITE_ROOT, "arp/");
        }
        File file = new File(result);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        result = RdfXmlTestBundle.class.getClassLoader().getResource(result).getFile();
        if (result.contains(".jar!/")) {
            try {
                return new URL("jar:" + result).openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return new FileInputStream(result);
    }

}
