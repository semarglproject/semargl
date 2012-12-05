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

package org.semarglproject.rdf;

public abstract class DataProcessor<T> {

    abstract void process(T reader) throws ParseException;

    abstract boolean isStreamFinished();

    abstract void endStream() throws ParseException;

    abstract void setBaseUri(String baseUri);

    public void process(T reader, String baseUri) throws ParseException {
        try {
            setBaseUri(baseUri);
            process(reader);
        } catch (ParseException e) {
            if (!isStreamFinished()) {
                endStream();
            }
            throw e;
        }
    }

}
