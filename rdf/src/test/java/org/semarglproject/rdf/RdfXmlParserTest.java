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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.semarglproject.rdf.RdfXmlTestBundle.TestCase;
import org.semarglproject.rdf.impl.ClerezzaTripleSink;
import org.semarglproject.rdf.impl.JenaTripleSink;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;

import static org.testng.Assert.assertTrue;

public final class RdfXmlParserTest {

    private static final String FAILURES_DIR = "target/failed/";
    private static final String TESTSUITE_DIR = "src/test/resources/";

    private static final String ARP_TESTSUITE_ROOT = "http://jcarroll.hpl.hp.com/arp-tests/";
    private static final String ARP_TESTSUITE_MANIFEST_URI = ARP_TESTSUITE_ROOT + "Manifest.rdf";

    private static final String W3C_TESTSUITE_ROOT = "http://www.w3.org/2000/10/rdf-tests/rdfcore/";
    private static final String W3C_TESTSUITE_MANIFEST_URI = W3C_TESTSUITE_ROOT + "Manifest.rdf";

    private Model model;
    private MGraph graph;
    private TurtleSerializerSink semarglTurtleSink;

    @BeforeClass(groups = { "Jena", "Clerezza", "Semargl-Turtle" })
    public static void cleanTargetDir() throws IOException {
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
    public static Object[][] getTestSuite() {
        List<TestCase> testCases = new RdfXmlTestBundle(getLocalPath(W3C_TESTSUITE_MANIFEST_URI,
                TESTSUITE_DIR), W3C_TESTSUITE_MANIFEST_URI, W3C_TESTSUITE_ROOT).getTestCases();
        testCases.addAll(new RdfXmlTestBundle(getLocalPath(ARP_TESTSUITE_MANIFEST_URI,
                TESTSUITE_DIR), ARP_TESTSUITE_MANIFEST_URI, ARP_TESTSUITE_ROOT).getTestCases());
        Object[][] result = new Object[testCases.size()][];
        for (int i = 0; i < testCases.size(); i++) {
            result[i] = new Object[] { testCases.get(i) };
        }
        return result;
    }

    @Test(dataProvider = "getTestSuite", groups = "Jena" )
    public void runW3CWithJenaSink(TestCase testCase) {
        runTestWith(testCase, new JenaTripleSink(model), new SaveToFileCallback() {
            @Override
            public void run(DataProcessor<Reader> dp, FileReader input,
                            String inputUri, FileWriter output) throws ParseException {
                dp.process(input, inputUri);
                model.write(output, "TURTLE");
            }
        });
    }

    @Test(dataProvider = "getTestSuite", groups = "Clerezza")
    public void runW3CWithClerezzaSink(TestCase testCase) {
        runTestWith(testCase, new ClerezzaTripleSink(graph), new SaveToFileCallback() {
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

    @Test(dataProvider = "getTestSuite", groups = "Semargl-Turtle")
    public void runW3CWithTurtleSink(TestCase testCase) {
        runTestWith(testCase, semarglTurtleSink, new SaveToFileCallback() {
            @Override
            public void run(DataProcessor<Reader> dp, FileReader input,
                            String inputUri, FileWriter output) throws ParseException {
                semarglTurtleSink.setWriter(output);
                dp.process(input, inputUri);
            }
        });
    }

    private void runTestWith(TestCase testCase, TripleSink sink, SaveToFileCallback callback) {
        String inputUri = testCase.getInput();
        String resultUri = testCase.getResult();

        File inputFile = new File(getLocalPath(inputUri, TESTSUITE_DIR));

        File outputFile = new File(getLocalPath(inputUri, FAILURES_DIR) + ".out.nt");
        outputFile.getParentFile().mkdirs();

        Model inputModel = ModelFactory.createDefaultModel();
        Model resultModel = ModelFactory.createDefaultModel();
        boolean success = true;
        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader();
            DataProcessor<Reader> dp = new SaxSource(xmlReader)
                    .streamingTo(new RdfXmlParser()
                            .streamingTo(sink)).build();
            FileReader input = new FileReader(inputFile);
            FileWriter output = new FileWriter(outputFile);
            try {
                callback.run(dp, input, inputUri, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
            if (outputFile.exists()) {
                inputModel.read(new FileInputStream(outputFile), inputUri, "TURTLE");
            }
            if (resultUri != null) {
                if (resultUri.endsWith(".rdf")) {
                    resultModel.read(new FileInputStream(getLocalPath(resultUri, TESTSUITE_DIR)),
                            inputUri, "RDF/XML");
                } else {
                    resultModel.read(new FileInputStream(getLocalPath(resultUri, TESTSUITE_DIR)),
                            inputUri, "N-TRIPLE");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        } catch (SAXException e) {
            e.printStackTrace();
            success = false;
        } catch (ParseException e) {
            System.err.println(">>> " + e.getMessage() + " (" + inputFile.getAbsolutePath() + ")");
            // ParseException means we should purge all collected triples, because document is not well formed
            if (outputFile != null) {
                try {
                    outputFile.createNewFile();
                } catch (IOException e1) {
                }
            }
        }

        success &= outputFile.exists() && inputModel.isIsomorphicWith(resultModel);
        if (success) {
            outputFile.delete();
        }
        assertTrue(success, "\ninput: " + getLocalPath(testCase.getInput(), TESTSUITE_DIR)
                + "\nresult: " + getLocalPath(testCase.getResult(), TESTSUITE_DIR) + "\n");
    }

    private static String getLocalPath(String uri, String base) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith(W3C_TESTSUITE_ROOT)) {
            return uri.replace(W3C_TESTSUITE_ROOT, base + "w3c/");
        }
        if (uri.startsWith(ARP_TESTSUITE_ROOT)) {
            return uri.replace(ARP_TESTSUITE_ROOT, base + "arp/");
        }
        return null;
    }

    private interface SaveToFileCallback {
        void run(DataProcessor<Reader> dp, FileReader input, String inputUri, FileWriter output) throws ParseException;
    }
}
