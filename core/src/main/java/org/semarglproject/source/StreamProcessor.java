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

/**
 * User API for stream processing pipelines. Automatically instantiates appropriate sources.
 * Provides processing and property setting methods. Supported properties:
 * <ul>
 *     <li>{@link #XML_READER_PROPERTY}</li>
 * </ul>
 */
public class StreamProcessor {

    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Allows to specify custom {@link org.xml.sax.XMLReader}.
     */
    public static final String XML_READER_PROPERTY = "http://semarglproject.org/core/properties/xml-parser";

    private final DataSink sink;
    private AbstractSource source;

    public StreamProcessor(DataSink sink) {
        this.sink = sink;
        this.source = null;
    }

    /**
     * Processes specified document's file using file path as base URI
     * @param file document's file
     * @throws ParseException
     */
    public void process(File file) throws ParseException {
        process(file, "file://" + file.getAbsolutePath());
    }

    /**
     * Processes specified document's file
     * @param file document's file
     * @param baseUri document's URI
     * @throws ParseException
     */
    public void process(File file, String baseUri) throws ParseException {
        if (source == null) {
            source = getSourceForSink(sink);
        }
        source.process(file, null, baseUri);
    }

    /**
     * Processes document pointed by specified URI
     * @param uri document's URI
     * @throws ParseException
     */
    public void process(String uri) throws ParseException {
        process(uri, uri);
    }

    /**
     * Processes document pointed by specified URI. Uses specified URI as document's base.
     * @param uri document's URI
     * @param baseUri document's URI
     * @throws ParseException
     */
    public void process(String uri, String baseUri) throws ParseException {
        if (source == null) {
            source = getSourceForSink(sink);
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
                source.process(inputStream, mimeType, baseUri);
            } finally {
                AbstractSource.closeQuietly(inputStream);
            }
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    /**
     * Processes stream input for document
     * @param inputStream document's input stream
     * @param baseUri document's base URI
     * @throws ParseException
     */
    public void process(InputStream inputStream, String baseUri) throws ParseException {
        source.process(inputStream, null, baseUri);
    }

    /**
     * Processes reader input for document's
     * @param reader document's reader
     * @param baseUri document's base URI
     * @throws ParseException
     */
    public void process(Reader reader, String baseUri) throws ParseException {
        if (source == null) {
            source = getSourceForSink(sink);
        }
        source.process(reader, null, baseUri);
    }

    private AbstractSource getSourceForSink(DataSink output) {
        if (output instanceof CharSink) {
            return new CharSource((CharSink) output);
        } else if (output instanceof SaxSink) {
            return new SaxSource((SaxSink) output);
        }
        return null;
    }

    /**
     * Key-value based settings. Property settings are passed to child sinks.
     * @param key property key
     * @param value property value
     * @return true if at least one sink understands specified property, false otherwise
     */
    public boolean setProperty(String key, Object value) {
        if (source == null) {
            source = getSourceForSink(sink);
        }
        return source.setProperty(key, value);
    }

}
