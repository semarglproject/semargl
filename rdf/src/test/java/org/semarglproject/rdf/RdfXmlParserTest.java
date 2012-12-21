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

import org.semarglproject.processor.SaxSource;
import org.semarglproject.processor.StreamProcessor;
import org.semarglproject.rdf.RdfXmlTestBundle.TestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static org.semarglproject.rdf.RdfXmlTestBundle.SaveToFileCallback;
import static org.semarglproject.rdf.RdfXmlTestBundle.runTestWith;

public final class RdfXmlParserTest {

    private TurtleSerializerSink semarglTurtleSink;
    private StreamProcessor<Reader> sp;

    @BeforeClass
    public void cleanTargetDir() throws IOException, SAXException {
        RdfXmlTestBundle.prepareTestDir();

        semarglTurtleSink = new TurtleSerializerSink();
        sp = SaxSource.streamingTo(RdfXmlParser.streamingTo(semarglTurtleSink));
    }

    @DataProvider
    public static Object[][] getTestSuite() {
        return RdfXmlTestBundle.getTestFiles();
    }

    @Test(dataProvider = "getTestSuite")
    public void runW3CWithTurtleSink(TestCase testCase) {
        runTestWith(testCase, new SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                semarglTurtleSink.setWriter(output);
                sp.process(input, inputUri);
            }
        });
    }

}
