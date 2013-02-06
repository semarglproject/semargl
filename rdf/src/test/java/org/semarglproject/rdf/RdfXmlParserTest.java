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

import org.semarglproject.sink.CharOutputSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.rdf.RdfXmlTestBundle.TestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public final class RdfXmlParserTest {

    private CharOutputSink charOutputSink;
    private StreamProcessor streamProcessorTtl;
    private StreamProcessor streamProcessorNt;

    @BeforeClass
    public void cleanTargetDir() throws IOException, SAXException {
        RdfXmlTestBundle.prepareTestDir();

        charOutputSink = new CharOutputSink("UTF-8");
        streamProcessorTtl = new StreamProcessor(RdfXmlParser.connect(TurtleSerializer.connect(charOutputSink)));
        streamProcessorNt = new StreamProcessor(RdfXmlParser.connect(NTriplesSerializer.connect(charOutputSink)));
    }

    @DataProvider
    public static Object[][] getTestSuite() {
        return RdfXmlTestBundle.getTestFiles();
    }

    @Test(dataProvider = "getTestSuite")
    public void runWithTurtleSink(TestCase testCase) {
        RdfXmlTestBundle.runTestWith(testCase, new RdfXmlTestBundle.SaveToFileCallback() {
            @Override
            public String run(Reader input, String inputUri, Writer output) throws ParseException {
                charOutputSink.connect(output);
                streamProcessorTtl.process(input, inputUri);
                return ".ttl";
            }
        });
    }

    @Test(dataProvider = "getTestSuite")
    public void runWithNTriplesSink(TestCase testCase) {
        RdfXmlTestBundle.runTestWith(testCase, new RdfXmlTestBundle.SaveToFileCallback() {
            @Override
            public String run(Reader input, String inputUri, Writer output) throws ParseException {
                charOutputSink.connect(output);
                streamProcessorNt.process(input, inputUri);
                return ".nt";
            }
        });
    }
}
