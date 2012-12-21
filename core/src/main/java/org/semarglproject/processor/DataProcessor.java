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

package org.semarglproject.processor;

import org.semarglproject.rdf.ParseException;
import org.semarglproject.sink.DataSink;

abstract class DataProcessor<T, S extends DataSink> implements StreamProcessor<T> {

    protected final S sink;
    private boolean streaming;

    protected DataProcessor(S sink) {
        this.sink = sink;
        streaming = true;
    }

    protected abstract void process(T reader) throws ParseException;

    @Override
    public void process(T reader, String baseUri) throws ParseException {
        try {
            setBaseUri(baseUri);
            process(reader);
        } catch (ParseException e) {
            if (!isStreaming()) {
                endStream();
            }
            throw e;
        }
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


    @Override
    public boolean setProperty(String key, Object value) {
        if (sink != null) {
            return sink.setProperty(key, value);
        }
        return false;
    }

}
