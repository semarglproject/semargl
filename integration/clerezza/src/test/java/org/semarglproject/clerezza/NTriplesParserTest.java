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

package org.semarglproject.clerezza;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.semarglproject.clerezza.core.sink.ClerezzaSink;
import org.semarglproject.processor.StreamProcessor;
import org.semarglproject.rdf.NTriplesParser;
import org.semarglproject.rdf.NTriplesTestBundle;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.processor.CharSource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public final class NTriplesParserTest {

    private MGraph graph;
    private StreamProcessor<Reader> sp;

    @BeforeClass
    public void init() {
        NTriplesTestBundle.prepareTestDir();

        UriRef graphUri = new UriRef("http://example.com/");
        TcManager tcManager = TcManager.getInstance();
        if (tcManager.listMGraphs().contains(graphUri)) {
            tcManager.deleteTripleCollection(graphUri);
        }
        graph = tcManager.createMGraph(graphUri);
        sp = CharSource.streamingTo(NTriplesParser.streamingTo(ClerezzaSink.to(graph)));
    }

    @BeforeMethod
    public void setUp() {
        if (graph != null) {
            graph.clear();
        }
    }

    @Test(dataProvider = "getTestFiles")
    public void NTriplesTestsClerezza(String caseName) throws Exception {
        NTriplesTestBundle.runTest(caseName, new NTriplesTestBundle.SaveToFileCallback() {
            @Override
            public void run(Reader input, String inputUri, Writer output) throws ParseException {
                try {
                    sp.process(input, inputUri);
                } finally {
                    OutputStream outputStream = new WriterOutputStream(output, "UTF-8");
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

    @DataProvider
    public Object[][] getTestFiles() throws IOException {
        return NTriplesTestBundle.getTestFiles();
    }
}
