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

package org.semarglproject.rdf.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.RdfXmlParser;
import org.semarglproject.rdf.RdfXmlTestBundle;
import org.semarglproject.rdf.RdfXmlTestBundle.TestCase;
import org.semarglproject.rdf.SaxSource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.Reader;
import java.io.Writer;

import static org.semarglproject.rdf.RdfXmlTestBundle.SaveToFileCallback;
import static org.semarglproject.rdf.RdfXmlTestBundle.runTestWith;

public final class RdfXmlParserTest {

    private Model model;
    private DataProcessor<Reader> dp;

    @BeforeClass
    public void init() throws SAXException {
        RdfXmlTestBundle.prepareTestDir();

        model = ModelFactory.createDefaultModel();
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        dp = new SaxSource(xmlReader).streamingTo(new RdfXmlParser().streamingTo(new JenaTripleSink(model))).build();
    }

    @BeforeMethod
    public void setUp() {
        model.removeAll();
    }

    @DataProvider
    public static Object[][] getTestSuite() {
        return RdfXmlTestBundle.getTestFiles();
    }

    @Test(dataProvider = "getTestSuite")
    public void runW3CWithJenaSink(TestCase testCase) {
        runTestWith(testCase, new SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                try {
                    dp.process(input, inputUri);
                } finally {
                    model.write(output, "TURTLE");
                }
            }
        });
    }

}
