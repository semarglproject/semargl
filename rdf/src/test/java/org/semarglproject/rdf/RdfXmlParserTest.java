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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semarglproject.ClerezzaSinkWrapper;
import org.semarglproject.JenaSinkWrapper;
import org.semarglproject.SinkWrapper;
import org.semarglproject.rdf.RdfXmlTestBundle.TestCase;
import org.testng.annotations.BeforeClass;
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
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import static org.testng.Assert.assertTrue;

public final class RdfXmlParserTest {

    private static final String FAILURES_DIR = "target/failed/";
    private static final String TESTSUITE_DIR = "src/test/resources/";

    private static final String ARP_TESTSUITE_ROOT = "http://jcarroll.hpl.hp.com/arp-tests/";
    private static final String ARP_TESTSUITE_MANIFEST_URI = ARP_TESTSUITE_ROOT + "Manifest.rdf";

    private static final String W3C_TESTSUITE_ROOT = "http://www.w3.org/2000/10/rdf-tests/rdfcore/";
    private static final String W3C_TESTSUITE_MANIFEST_URI = W3C_TESTSUITE_ROOT + "Manifest.rdf";

    private final SinkWrapper clerezzaWrapper = new ClerezzaSinkWrapper();
    private final SinkWrapper jenaWrapper = new JenaSinkWrapper();
    private final SinkWrapper turtleSerializerWrapper = new SinkWrapper<Reader>() {

        private TurtleSerializerSink sink = new TurtleSerializerSink();

        @Override
        public TripleSink getSink() {
            return sink;
        }

        @Override
        public void reset() {
        }

        @Override
        public void process(DataProcessor<Reader> dp, Reader input, String baseUri, Writer output)
                throws ParseException, IOException {
            sink.setWriter(output);
            dp.process(input, baseUri);
        }
    };

    @BeforeClass
    public static void cleanTargetDir() throws IOException {
        File failuresDir = new File(FAILURES_DIR);
        FileUtils.deleteDirectory(failuresDir);
        failuresDir.mkdirs();
    }

    @DataProvider(name = "W3C-Tests-Provider")
    public static Object[][] getW3CTestSuite() {
        Collection<TestCase> testCases = new RdfXmlTestBundle(getLocalPath(W3C_TESTSUITE_MANIFEST_URI,
                TESTSUITE_DIR), W3C_TESTSUITE_MANIFEST_URI, W3C_TESTSUITE_ROOT).getTestCases();
        Object[][] result = new Object[testCases.size()][];
        int i = 0;
        for (TestCase testCase : testCases) {
            result[i++] = new Object[] { testCase };
        }
        return result;
    }

    @DataProvider(name = "ARP-Tests-Provider")
    public static Object[][] getARPTestSuite() {
        Collection<TestCase> testCases = new RdfXmlTestBundle(getLocalPath(ARP_TESTSUITE_MANIFEST_URI,
                TESTSUITE_DIR), ARP_TESTSUITE_MANIFEST_URI, ARP_TESTSUITE_ROOT).getTestCases();
        Object[][] result = new Object[testCases.size()][];
        int i = 0;
        for (TestCase testCase : testCases) {
            result[i++] = new Object[] { testCase };
        }
        return result;
    }

    @Test(dataProvider = "W3C-Tests-Provider")
    public void runW3CWithJenaSink(TestCase testCase) {
        runTestWith(testCase, jenaWrapper);
    }

    @Test(dataProvider = "ARP-Tests-Provider")
    public void runARPWithJenaSink(TestCase testCase) {
        runTestWith(testCase, jenaWrapper);
    }

    @Test(dataProvider = "W3C-Tests-Provider")
    public void runW3CWithClerezzaSink(TestCase testCase) {
        runTestWith(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "ARP-Tests-Provider")
    public void runARPWithClerezzaSink(TestCase testCase) {
        runTestWith(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "W3C-Tests-Provider")
    public void runW3CWithTurtleSink(TestCase testCase) {
        runTestWith(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "ARP-Tests-Provider")
    public void runARPWithTurtleSink(TestCase testCase) {
        runTestWith(testCase, turtleSerializerWrapper);
    }

    private void runTestWith(TestCase testCase, SinkWrapper wrapper) {
        String inputUri = testCase.getInput();
        String resultUri = testCase.getResult();

        File inputFile = new File(getLocalPath(inputUri, TESTSUITE_DIR));

        File outputFile = new File(getLocalPath(inputUri, FAILURES_DIR) + ".out.nt");
        outputFile.getParentFile().mkdirs();

        Model inputModel = ModelFactory.createDefaultModel();
        Model resultModel = ModelFactory.createDefaultModel();
        boolean success = true;
        try {
            extract(inputFile, inputUri, outputFile, wrapper);
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

    private void extract(File inputFile, String baseUri, File outputFile, SinkWrapper wrapper)
            throws IOException, SAXException, ParseException {
        wrapper.reset();
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        DataProcessor<Reader> dp = new SaxSource(xmlReader)
                .streamingTo(new RdfXmlParser()
                        .streamingTo(wrapper.getSink())).build();
        FileReader input = new FileReader(inputFile);
        FileWriter output = new FileWriter(outputFile);
        try {
            wrapper.process(dp, input, baseUri, output);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
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
}
