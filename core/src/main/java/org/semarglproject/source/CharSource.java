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

package org.semarglproject.source;

import org.semarglproject.rdf.ParseException;
import org.semarglproject.sink.CharSink;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

final class CharSource extends AbstractSource<CharSink> {

    CharSource(CharSink sink) {
        super(sink);
    }

    @Override
    public void process(File file, String mimeType, String baseUri) throws ParseException {
        FileReader reader;
        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            throw new ParseException(e);
        }
        try {
            processInternal(reader, baseUri);
        } finally {
            closeQuietly(reader);
        }
    }

    @Override
    public void process(Reader reader, String mimeType, String baseUri) throws ParseException {
        processInternal(reader, baseUri);
    }

    private void processInternal(Reader source, String baseUri) throws ParseException {
        try {
            setBaseUri(baseUri);
            startStream();
            sink.read(source);
            endStream();
        } catch (ParseException e) {
            if (!isStreaming()) {
                endStream();
            }
            throw e;
        }
    }

}