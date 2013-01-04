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
package org.semarglproject.jena.core.sink;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.Lock;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.sink.TripleSink;

/**
 * Implementation if {@link TripleSink} which feeds triples from Semargl's pipeline to Jena's {@link Model}.
 * <p>
 *     List of supported options:
 *     <ul>
 *         <li>{@link #OUTPUT_MODEL_PROPERTY}</li>
 *     </ul>
 * </p>
 */
public final class JenaSink extends AbstractJenaSink {

    private static final int DEFAULT_BATCH_SIZE = 512;

    private final int batchSize;

    private Triple[] triples;
    private int triplesSize;

    private JenaSink(Model model, int batchSize) {
        super(model);
        this.batchSize = batchSize;
    }
    /**
     * Instantiates sink for specified Jena {@link Model}
     * @param model model to sink triples to
     * @return new instance of Jena sink
     */
    public static TripleSink connect(Model model) {
        return new JenaSink(model, DEFAULT_BATCH_SIZE);
    }

    private void newBatch() {
        triples = new Triple[batchSize];
        triplesSize = 0;
    }

    @Override
    protected void addTriple(Node subj, Node pred, Node obj) {
        triples[triplesSize++] = new Triple(subj, pred, obj);
        if (triplesSize == batchSize) {
            model.enterCriticalSection(Lock.WRITE);
            model.getGraph().getBulkUpdateHandler().add(triples);
            model.leaveCriticalSection();
            newBatch();
        }
    }

    @Override
    public void startStream() throws ParseException {
        newBatch();
    }

    @Override
    public void endStream() throws ParseException {
        if (triplesSize == 0) {
            return;
        }
        Triple[] dummy = new Triple[triplesSize];
        System.arraycopy(triples, 0, dummy, 0, triplesSize);
        model.enterCriticalSection(Lock.WRITE);
        model.getGraph().getBulkUpdateHandler().add(dummy);
        model.leaveCriticalSection();
    }

    @Override
    public void setBaseUri(String baseUri) {
    }
}
