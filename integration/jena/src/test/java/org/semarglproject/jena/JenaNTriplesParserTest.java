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
package org.semarglproject.jena;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.jena.core.sink.JenaSink;
import org.semarglproject.rdf.NTriplesParser;
import org.semarglproject.rdf.NTriplesParserTest;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.source.StreamProcessor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public final class JenaNTriplesParserTest {

    private Model model;
    private StreamProcessor sp;
    private NTriplesParserTest nTriplesParserTest;

    @BeforeClass
    public void init() {
        nTriplesParserTest = new NTriplesParserTest();
        nTriplesParserTest.init();
        model = ModelFactory.createDefaultModel();
        sp = new StreamProcessor(NTriplesParser.connect(JenaSink.connect(model)));
    }

    @BeforeMethod
    public void setUp() {
        model.removeAll();
    }

    @DataProvider
    public Object[][] getTestSuite() throws IOException {
        return nTriplesParserTest.getTestSuite();
    }

    @Test(dataProvider = "getTestSuite")
    public void runTestSuite(NTriplesParserTest.TestCase testCase) throws Exception {
        nTriplesParserTest.runTest(testCase, new NTriplesParserTest.SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                try {
                    sp.process(input, inputUri);
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
