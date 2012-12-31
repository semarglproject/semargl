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
import org.semarglproject.sink.DataSink;
import org.semarglproject.sink.SaxSink;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public final class StreamProcessor {

    public static final String XML_READER_PROPERTY = "http://semarglproject.org/core/properties/xml-parser";

    private final DataSink sink;
    private AbstractSource streamProcessor;

    public StreamProcessor(DataSink sink) {
        this.sink = sink;
        this.streamProcessor = null;
    }

    public void process(File file) throws ParseException {
        process(file, "file://" + file.getAbsolutePath());
    }

    public void process(File file, String baseUri) throws ParseException {
        if (streamProcessor == null) {
            streamProcessor = getSourceStreamForOutput(sink);
        }
        streamProcessor.process(file, null, baseUri);
    }

    public void process(String uri) throws ParseException {
        process(uri, uri);
    }

    public void process(String uri, String baseUri) throws ParseException {
        if (streamProcessor == null) {
            streamProcessor = getSourceStreamForOutput(sink);
        }

        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            throw new ParseException(e);
        }
        try {
            URLConnection urlConnection = url.openConnection();
            String mimeType = urlConnection.getContentType();
            InputStream inputStream = urlConnection.getInputStream();
            try {
                streamProcessor.process(inputStream, mimeType, baseUri);
            } finally {
                AbstractSource.closeQuietly(inputStream);
            }
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    public void process(InputStream inputStream, String mimeType, String baseUri) throws ParseException {
        streamProcessor.process(inputStream, mimeType, baseUri);
    }

    public void process(Reader reader, String baseUri) throws ParseException {
        if (streamProcessor == null) {
            streamProcessor = getSourceStreamForOutput(sink);
        }
        streamProcessor.process(reader, null, baseUri);
    }

    private AbstractSource getSourceStreamForOutput(DataSink output) {
        if (output instanceof CharSink) {
            return new CharSource((CharSink) output);
        } else if (output instanceof SaxSink) {
            return new SaxSource((SaxSink) output);
        }
        return null;
    }

    public boolean setProperty(String key, Object value) {
        if (streamProcessor == null) {
            streamProcessor = getSourceStreamForOutput(sink);
        }
        return streamProcessor.setProperty(key, value);
    }

}
