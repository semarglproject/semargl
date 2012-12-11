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
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.semarglproject.rdf.impl.ClerezzaTripleSink;
import org.semarglproject.rdf.impl.JenaTripleSink;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertTrue;

public final class NTriplesParserTest {

    private static final String FAILURES_DIR = "target/n3_failed/";

    private static final String TESTSUITE_DIR = "src/test/resources/w3c";
    private static final String RDF_TEST_SUITE_ROOT = "http://www.w3.org/2000/10/rdf-tests/rdfcore";

    private Model model;
    private MGraph graph;
    private TurtleSerializerSink semarglTurtleSink;

    @BeforeClass(groups = { "Jena", "Clerezza", "Semargl-Turtle" })
    public void cleanTargetDir() throws IOException {
        File failuresDir = new File(FAILURES_DIR);
        FileUtils.deleteDirectory(failuresDir);
        failuresDir.mkdirs();
    }

    @BeforeGroups(groups = "Jena")
    public void initJena() {
        model = ModelFactory.createDefaultModel();
    }

    @BeforeGroups(groups = "Clerezza")
    public void initClerezza() {
        UriRef graphUri = new UriRef("http://example.com/");
        TcManager MANAGER = TcManager.getInstance();
        if (MANAGER.listMGraphs().contains(graphUri)) {
            MANAGER.deleteTripleCollection(graphUri);
        }
        graph = MANAGER.createMGraph(graphUri);
    }

    @BeforeGroups(groups = "Semargl-Turtle")
    public void initSemarglTertle() {
        semarglTurtleSink = new TurtleSerializerSink();
    }

    @BeforeMethod(groups = "Jena")
    public void setUpJena() {
        model.removeAll();
    }

    @BeforeMethod(groups = "Clerezza")
    public void setUpClerezza() {
        if (graph != null) {
            graph.clear();
        }
    }

    @DataProvider
    public Object[][] getTestFiles() {
        List<String> result = new ArrayList<String>();
        String queryStr = null;
        String manifestUri = RDF_TEST_SUITE_ROOT + "/Manifest.rdf";
        Model graph = ModelFactory.createDefaultModel();

        try {
            graph.read(new FileInputStream("src/test/resources/w3c/Manifest.rdf"), manifestUri, "RDF/XML");
            queryStr = FileUtils.readFileToString(new File(
                    "src/test/resources/fetch_ntriples_tests.sparql"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Query query = QueryFactory.create(queryStr, manifestUri);
        QueryExecution qe = QueryExecutionFactory.create(query, graph);
        ResultSet rs = qe.execSelect();
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

    @Test(dataProvider = "getTestFiles", groups = "Jena")
    public void NTriplesTestsJena(String caseName) throws Exception {
        runTestBundle(caseName, new JenaTripleSink(model), new SaveToFileCallback() {
            @Override
            public void run(DataProcessor<Reader> dp, FileReader input,
                            String inputUri, FileWriter output) throws ParseException {
                dp.process(input, inputUri);
                model.write(output, "TURTLE");
            }
        });
    }

    @Test(dataProvider = "getTestFiles", groups = "Clerezza")
    public void NTriplesTestsClerezza(String caseName) throws Exception {
        runTestBundle(caseName, new ClerezzaTripleSink(graph), new SaveToFileCallback() {
            @Override
            public void run(DataProcessor<Reader> dp, FileReader input,
                            String inputUri, FileWriter output) throws ParseException {
                dp.process(input, inputUri);
                if (graph != null) {
                    OutputStream outputStream = new WriterOutputStream(output);
                    try {
                        Serializer serializer = Serializer.getInstance();
                        serializer.serialize(outputStream, graph, "text/turtle");
                    } finally {
                        IOUtils.closeQuietly(outputStream);
                    }
                }
            }
        });
    }

    @Test(dataProvider = "getTestFiles", groups = "Semargl-Turtle")
    public void NTriplesTestsTurtle(String caseName) throws Exception {
        runTestBundle(caseName, semarglTurtleSink, new SaveToFileCallback() {
            @Override
            public void run(DataProcessor<Reader> dp, FileReader input,
                            String inputUri, FileWriter output) throws ParseException {
                semarglTurtleSink.setWriter(output);
                dp.process(input, inputUri);
            }
        });
    }

    void runTestBundle(String caseName, TripleSink sink, SaveToFileCallback callback) throws IOException {
        File docFile = new File(caseName.replace(RDF_TEST_SUITE_ROOT, TESTSUITE_DIR));

        File outputFile = new File(FAILURES_DIR, caseName.substring(caseName.lastIndexOf('/') + 1));
        Model outputModel = ModelFactory.createDefaultModel();
        Model resultModel = ModelFactory.createDefaultModel();

        boolean invalidInput = false;
        FileInputStream inputStream = new FileInputStream(docFile);
        try {
            resultModel.read(inputStream, caseName, "N-TRIPLE");
        } catch (Exception e) {
            invalidInput = true;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        try {
            DataProcessor<Reader> dp = new CharSource()
                    .streamingTo(new NTriplesParser()
                            .streamingTo(sink)).build();
            FileReader input = new FileReader(docFile);
            FileWriter output = new FileWriter(outputFile);
            try {
                callback.run(dp, input, caseName, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        } catch (ParseException e) {
            if (invalidInput) {
                outputFile.delete();
                return;
            }
        }

        inputStream = new FileInputStream(outputFile);
        try {
            outputModel.read(inputStream, caseName, "TURTLE");
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        boolean success = outputModel.isIsomorphicWith(resultModel);
        if (success) {
            outputFile.delete();
        }
        assertTrue(success);
    }

    private interface SaveToFileCallback {
        void run(DataProcessor<Reader> dp, FileReader input, String inputUri, FileWriter output) throws ParseException;
    }
}
