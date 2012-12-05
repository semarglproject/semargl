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

import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.rdf.TurtleSerializerSink;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

public final class TurtleSerializerSinkWrapper implements SinkWrapper<Reader> {

    private TurtleSerializerSink sink = new TurtleSerializerSink();

    @Override
    public TripleSink getSink() {
        return sink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void process(DataProcessor<Reader> dp, File inputFile, String baseUri, File outputFile)
            throws ParseException, IOException {
        FileReader reader = new FileReader(inputFile);
        FileWriter writer = new FileWriter(outputFile);
        try {
            sink.setWriter(writer);
            dp.process(reader, baseUri);
        } finally {
            TestUtils.closeQuietly(reader);
            TestUtils.closeQuietly(writer);
        }
    }
}