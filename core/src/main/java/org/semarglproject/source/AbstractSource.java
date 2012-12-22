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
import org.semarglproject.sink.DataSink;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

abstract class AbstractSource<S extends DataSink> {

    protected final S sink;
    private boolean streaming;

    public abstract void process(File file, String mimeType, String baseUri) throws ParseException;

    public abstract void process(Reader reader, String mimeType, String baseUri) throws ParseException;

    public void process(InputStream inputStream, String mimeType, String baseUri) throws ParseException {
        InputStreamReader reader;
        reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
        try {
            process(reader, mimeType, baseUri);
        } finally {
            closeQuietly(reader);
        }
    }

    protected AbstractSource(S sink) {
        this.sink = sink;
        this.streaming = false;
    }

    protected void setBaseUri(String baseUri) {
        sink.setBaseUri(baseUri);
    }

    protected boolean isStreaming() {
        return streaming;
    }

    protected void startStream() {
        sink.startStream();
        streaming = false;
    }

    protected void endStream() throws ParseException {
        streaming = true;
        sink.endStream();
    }

    public boolean setProperty(String key, Object value) {
        if (sink != null) {
            return sink.setProperty(key, value);
        }
        return false;
    }

    static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

}
