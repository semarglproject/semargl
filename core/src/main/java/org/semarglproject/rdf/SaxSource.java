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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.Reader;

public class SaxSource implements MainSource<Reader, SaxSink> {

    private SaxSink sink = null;
    private XMLReader xmlReader = null;

    public SaxSource(XMLReader xmlReader) {
        this.xmlReader = xmlReader;
    }

    @Override
    public DataProcessor<Reader> build() {
        return new DataProcessor<Reader>() {
            private boolean streamFinished = true;

            @Override
            void process(Reader source) throws ParseException {
                xmlReader.setContentHandler(sink);
                try {
                    xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", sink);
                } catch (SAXException e) {
                    // nothing
                }
                try {
                    streamFinished = false;
                    sink.startStream();
                    xmlReader.parse(new InputSource(source));
                    streamFinished = true;
                } catch (SAXException e) {
                    ParseException e2 = sink.processException(e);
                    endStream();
                    throw e2;
                } catch (IOException e) {
                    streamFinished = true;
                    throw new ParseException(e);
                } finally {
                    sink.endStream();
                }

            }

            @Override
            boolean isStreamFinished() {
                return streamFinished;
            }

            @Override
            void endStream() throws ParseException {
                streamFinished = true;
                try {
                    sink.endDocument();
                } catch (SAXException e) {
                    throw new ParseException(e);
                }
            }

            @Override
            void setBaseUri(String baseUri) {
                sink.setBaseUri(baseUri);
            }
        };
    }

    @Override
    public SaxSource streamingTo(SaxSink sink) {
        this.sink = sink;
        return this;
    }

}
