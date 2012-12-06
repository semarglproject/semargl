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

package org.semarglproject;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.commons.io.output.WriterOutputStream;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.rdf.impl.ClerezzaTripleSink;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public final class ClerezzaSinkWrapper implements SinkWrapper<Reader> {
    private static final TcManager MANAGER = TcManager.getInstance();
    private MGraph graph = null;

    @Override
    public TripleSink getSink() {
        UriRef graphUri = new UriRef("http://example.com/");
        if (MANAGER.listMGraphs().contains(graphUri)) {
            MANAGER.deleteTripleCollection(graphUri);
        }
        graph = MANAGER.createMGraph(graphUri);
        return new ClerezzaTripleSink(graph);
    }

    @Override
    public void reset() {
        if (graph != null) {
            graph.clear();
        }
    }

    @Override
    public void process(DataProcessor<Reader> dp, Reader input, String baseUri, Writer output)
            throws ParseException, IOException {
        dp.process(input, baseUri);
        if (graph != null) {
            OutputStream outputStream = new WriterOutputStream(output);
            try {
                TestUtils.dumpToStream(graph, outputStream, "text/turtle");
            } finally {
                TestUtils.closeQuietly(outputStream);
            }
        }
    }

}