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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.rdf.impl.JenaTripleSink;

import java.io.OutputStream;

public class JenaSinkWrapper implements SinkWrapper {
    private final Model model = ModelFactory.createDefaultModel();

    @Override
    public void dumpToStream(OutputStream outputStream) {
        model.write(outputStream, "TURTLE");
    }

    @Override
    public TripleSink getSink() {
        return new JenaTripleSink(model);
    }

    @Override
    public void reset() {
        model.removeAll();
    }
}
