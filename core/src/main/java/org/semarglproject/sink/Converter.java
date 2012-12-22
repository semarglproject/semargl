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

package org.semarglproject.sink;

import org.semarglproject.rdf.ParseException;

public abstract class Converter<T extends DataSink, S extends DataSink> implements DataSink {
    protected S sink;

    @Override
    public void startStream() throws ParseException {
        sink.startStream();
    }

    @Override
    public void endStream() throws ParseException {
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
