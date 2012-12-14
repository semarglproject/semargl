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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public final class NTriplesTestBundle {

    private static final String FAILURES_DIR = "target/n3_failed/";
    private static final String RDF_TEST_SUITE_ROOT = "http://www.w3.org/2000/10/rdf-tests/rdfcore/";

    public interface SaveToFileCallback {
        void run(Reader input, String inputUri, Writer output) throws ParseException;
    }

    public static Object[][] getTestFiles() throws IOException {
        String manifestUri = RDF_TEST_SUITE_ROOT + "/Manifest.rdf";
        Model graph = ModelFactory.createDefaultModel();

        graph.read(openStreamForResource("w3c/Manifest.rdf"), manifestUri, "RDF/XML");
        String queryStr = IOUtils.toString(openStreamForResource("fetch_ntriples_tests.sparql"));

        Query query = QueryFactory.create(queryStr, manifestUri);
        QueryExecution qe = QueryExecutionFactory.create(query, graph);
        ResultSet rs = qe.execSelect();

        List<String> result = new ArrayList<String>();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String testName = qs.getResource("ntriples_file").getURI();
            result.add(testName);
        }
        qe.close();
        int i = 0;
        Object[][] res = new Object[result.size()][];
        for (String str : result) {
            res[i++] = new Object[] { str };
        }
        return res;
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

    public static void runTest(String testUrl, SaveToFileCallback callback) {
        String resultFileName = testUrl.substring(testUrl.lastIndexOf('/') + 1).replace(".nt", ".ttl");
        String resultFilePath = FAILURES_DIR + resultFileName;
        try {
            Reader input = new InputStreamReader(openStreamForResource(testUrl));
            Writer output = new FileWriter(resultFilePath);
            try {
                callback.run(input, testUrl, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        } catch (ParseException e) {
            fail();
        } catch (IOException e) {
            fail();
        }

        try {
            Model result = createModelFromFile(resultFilePath, testUrl);
            Model expected = createModelFromFile(testUrl, testUrl);
            assertTrue(result.isIsomorphicWith(expected));
        } catch (FileNotFoundException e) {
            fail();
        }
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
        if (uri.startsWith(RDF_TEST_SUITE_ROOT)) {
            result = uri.replace(RDF_TEST_SUITE_ROOT, "w3c/");
        }
        File file = new File(uri);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        result = NTriplesTestBundle.class.getClassLoader().getResource(result).getFile();
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
