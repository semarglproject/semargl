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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semarglproject.ClerezzaSinkWrapper;
import org.semarglproject.JenaSinkWrapper;
import org.semarglproject.SinkWrapper;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.rdf.TurtleSerializerSink;
import org.semarglproject.rdf.rdfa.RdfaTestBundle.TestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Automatically downloads and runs test suite from http://rdfa.info
 *
 * Test cases:
 * - RDFa 1.0: XHTML1, SVG
 * - RDFa 1.1: HTML4, XHTML1, HTML5, XML, SVG
 */
public final class RdfaParserTest {

    private static final String FAILURES_DIR = "target/failed/";

    private static final String TESTSUITE_DIR = "src/test/resources/rdfa-testsuite/";

    private static final String RDFA_TESTSUITE_ROOT = "http://rdfa.info/test-suite/test-cases/";
    private static final String RDFA_TESTSUITE_MANIFEST_URI = RDFA_TESTSUITE_ROOT + "manifest.ttl";

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
    public static void cleanTargetDir() {
        File failuresDir = new File(FAILURES_DIR);
        deleteDir(failuresDir);
        failuresDir.mkdirs();
    }

    @DataProvider
    public static Object[][] getRdfa10Xhtml1TestSuite() {
        return getTestSuite("rdfa1.0", "xhtml1");
    }

    @DataProvider
    public static Object[][] getRdfa10SvgTestSuite() {
        return getTestSuite("rdfa1.0", "svg");
    }

    @DataProvider
    public static Object[][] getRdfa11Html4TestSuite() {
        return getTestSuite("rdfa1.1", "html4");
    }

    @DataProvider
    public static Object[][] getRdfa11Xhtml1TestSuite() {
        return getTestSuite("rdfa1.1", "xhtml1");
    }

    @DataProvider
    public static Object[][] getRdfa11Html5TestSuite() {
        return getTestSuite("rdfa1.1", "html5");
    }

    @DataProvider
    public static Object[][] getRdfa11XmlTestSuite() {
        return getTestSuite("rdfa1.1", "xml");
    }

    @DataProvider
    public static Object[][] getRdfa11SvgTestSuite() {
        return getTestSuite("rdfa1.1", "svg");
    }

    private static Object[][] getTestSuite(String rdfaVersion, String docFormat) {
        Collection<TestCase> testCases = new RdfaTestBundle(getLocalPath(RDFA_TESTSUITE_MANIFEST_URI,
                TESTSUITE_DIR), RDFA_TESTSUITE_MANIFEST_URI, rdfaVersion, docFormat).getTestCases();
        Object[][] result = new Object[testCases.size()][];
        int i = 0;
        for (TestCase testCase : testCases) {
            result[i++] = new Object[]{testCase};
        }
        return result;
    }

    /*
     * ClerezzaTripleSink
     */

    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
    public void Rdfa10Xhtml1TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "getRdfa10SvgTestSuite")
    public void Rdfa10SvgTestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "getRdfa11Html4TestSuite")
    public void Rdfa11Html4TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
    public void Rdfa11Xhtml1TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "getRdfa11Html5TestSuite")
    public void Rdfa11Html5TestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "getRdfa11XmlTestSuite")
    public void Rdfa11XmlTestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void Rdfa11SvgTestsClerezza(TestCase testCase) {
        runTestBundle(testCase, clerezzaWrapper);
    }

    /*
     * JenaTripleSink
     */

    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
    public void Rdfa10Xhtml1TestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    @Test(dataProvider = "getRdfa10SvgTestSuite")
    public void Rdfa10SvgTestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    @Test(dataProvider = "getRdfa11Html4TestSuite")
    public void Rdfa11Html4TestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
    public void Rdfa11Xhtml1TestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    @Test(dataProvider = "getRdfa11Html5TestSuite")
    public void Rdfa11Html5TestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    @Test(dataProvider = "getRdfa11XmlTestSuite")
    public void Rdfa11XmlTestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void Rdfa11SvgTestsJena(TestCase testCase) {
        runTestBundle(testCase, jenaWrapper);
    }

    /*
     * TurtleSerializerSink
     */

    @Test(dataProvider = "getRdfa10Xhtml1TestSuite")
    public void Rdfa10Xhtml1TestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "getRdfa10SvgTestSuite")
    public void Rdfa10SvgTestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "getRdfa11Html4TestSuite")
    public void Rdfa11Html4TestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "getRdfa11Xhtml1TestSuite")
    public void Rdfa11Xhtml1TestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "getRdfa11Html5TestSuite")
    public void Rdfa11Html5TestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "getRdfa11XmlTestSuite")
    public void Rdfa11XmlTestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    @Test(dataProvider = "getRdfa11SvgTestSuite")
    public void Rdfa11SvgTestsTurtle(TestCase testCase) {
        runTestBundle(testCase, turtleSerializerWrapper);
    }

    void runTestBundle(TestCase testCase, SinkWrapper wrapper) {
        String inputUri = testCase.getInput();
        String resultUri = testCase.getResult();

        File inputFile = new File(getLocalPath(inputUri, TESTSUITE_DIR));
        File resultFile = new File(getLocalPath(resultUri, TESTSUITE_DIR));

        File outputFile = new File(getLocalPath(inputUri, FAILURES_DIR) + ".out.nt");
        outputFile.getParentFile().mkdirs();

        try {
            downloadFile(inputUri, inputFile, false);
            downloadFile(resultUri, resultFile, false);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        boolean success;
        try {
            extract(inputFile, inputUri, outputFile, wrapper);

            Query query = QueryFactory.create(FileUtils.readFileToString(resultFile));

            Model resultModel = ModelFactory.createDefaultModel();
            resultModel.read(new FileInputStream(outputFile), inputUri, "TURTLE");

            QueryExecution qe = QueryExecutionFactory.create(query, resultModel);
            success = qe.execAsk() == testCase.getExpectedResult();
            qe.close();
        } catch (Exception e) {
            System.out.println("Running test case " + inputFile.getAbsolutePath());
            success = false;
            e.printStackTrace();
        }
        if (success) {
            outputFile.delete();
        }
        assertTrue(success);
    }

    private static String getLocalPath(String uri, String base) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith(RDFA_TESTSUITE_ROOT)) {
            return uri.replace(RDFA_TESTSUITE_ROOT, base);
        }
        return null;
    }

    private void extract(File inputFile, String baseUri, File outputFile, SinkWrapper wrapper) throws IOException, SAXException {
        wrapper.reset();
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DataProcessor<Reader> dp = new SaxSource(reader)
                    .streamingTo(new RdfaParser()
                            .streamingTo(wrapper.getSink())).build();

            FileReader input = new FileReader(inputFile);
            FileWriter output = new FileWriter(outputFile);
            try {
                wrapper.process(dp, input, baseUri, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        } catch (ParseException e) {
            System.out.println(">>> " + e.getMessage() + " (" + inputFile.getAbsolutePath() + ")");
        }
    }

    private static void deleteDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDir(child);
                }
            }
        }
        dir.delete();
    }

    private static void downloadFile(String sourceUrl, File dest, boolean forced) throws IOException {
        if (!forced && dest.exists()) {
            return;
        }
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        URL url = new URL(sourceUrl);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(dest);
        FileChannel channel = fos.getChannel();
        try {
            channel.transferFrom(rbc, 0, 1 << 24);
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }

}
