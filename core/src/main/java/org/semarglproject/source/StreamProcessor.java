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
import org.xml.sax.XMLReader;

import java.io.InputStream;
import java.io.Reader;

/**
 * Simple pipeline managing wrapper. Automatically instantiates source appropriate for specified sink.
 * Provides processing and setup methods.
 * <p>List of supported properties:
 *     <ul>
 *         <li>{@link #XML_READER_PROPERTY}</li>
 *     </ul>
 * </p>
 */
public final class StreamProcessor extends BaseStreamProcessor {

    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Allows to specify custom {@link org.xml.sax.XMLReader} used with SAX parsers.
     */
    public static final String XML_READER_PROPERTY = "http://semarglproject.org/core/properties/xml-parser";

    private final DataSink sink;
    private final AbstractSource source;

    public StreamProcessor(DataSink sink) {
        this.sink = sink;
        this.source = createSourceForSink(sink);
    }

    @Override
    public void processInternal(InputStream inputStream, String mimeType, String baseUri) throws ParseException {
        source.process(inputStream, mimeType, baseUri);
    }

    @Override
    protected void startStream() throws ParseException {
        sink.startStream();
    }

    @Override
    protected void endStream() throws ParseException {
        sink.endStream();
    }

    @Override
    public void processInternal(Reader reader, String mimeType, String baseUri) throws ParseException {
        source.process(reader, mimeType, baseUri);
    }

    @Override
    public boolean setProperty(String key, Object value) {
        boolean result = false;
        if (XML_READER_PROPERTY.equals(key) && value instanceof XMLReader && source instanceof SaxSource) {
            ((SaxSource) source).setXmlReader((XMLReader) value);
            result = true;
        }
        return sink.setProperty(key, value) || result;
    }

}
