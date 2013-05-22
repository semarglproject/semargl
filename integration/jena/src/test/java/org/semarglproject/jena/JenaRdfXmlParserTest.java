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
package org.semarglproject.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.jena.core.sink.JenaSink;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.RdfXmlParser;
import org.semarglproject.rdf.RdfXmlParserTest;
import org.semarglproject.source.StreamProcessor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import static org.semarglproject.rdf.RdfXmlParserTest.SaveToFileCallback;

public final class JenaRdfXmlParserTest {

    private Model model;
    private StreamProcessor streamProcessor;
    private RdfXmlParserTest rdfXmlParserTest;

    @BeforeClass
    public void init() {
        rdfXmlParserTest = new RdfXmlParserTest();
        rdfXmlParserTest.init();
        model = ModelFactory.createDefaultModel();
        streamProcessor = new StreamProcessor(RdfXmlParser.connect(JenaSink.connect(model)));
    }

    @BeforeMethod
    public void setUp() {
        model.removeAll();
    }

    @DataProvider
    public Object[][] getTestSuite() throws IOException {
        return rdfXmlParserTest.getTestSuite();
    }

    @Test(dataProvider = "getTestSuite")
    public void runTestSuite(RdfXmlParserTest.TestCase testCase) {
        rdfXmlParserTest.runTest(testCase, new SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                try {
                    streamProcessor.process(input, inputUri);
                } finally {
                    model.write(output, "TURTLE");
                }
            }

            @Override
            public String getOutputFileExt() {
                return "ttl";
            }
        });
    }

}
