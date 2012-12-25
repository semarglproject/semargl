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
import org.semarglproject.sink.SaxSink;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

final class SaxSource extends AbstractSource<SaxSink> {

    private XMLReader xmlReader = null;

    SaxSource(SaxSink sink) {
        super(sink);
    }

    @Override
    public void process(File file, String mimeType, String baseUri) throws ParseException, FileNotFoundException {
        setBaseUri(baseUri);
        FileReader reader = new FileReader(file);
        try {
            processInternal(reader);
        } finally {
            closeQuietly(reader);
        }
    }

    @Override
    public void process(Reader reader, String mimeType, String baseUri) throws ParseException {
        try {
            setBaseUri(baseUri);
            processInternal(reader);
        } catch (ParseException e) {
            if (!isStreaming()) {
                endStream();
            }
            throw e;
        }
    }

    private void processInternal(Reader source) throws ParseException {
        if (xmlReader == null) {
            try {
                xmlReader = XMLReaderFactory.createXMLReader();
                xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (SAXException e) {
                throw new ParseException("Can not create SaxSource with default XMLReader implementation", e);
            }
        }
        xmlReader.setContentHandler(sink);
        try {
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", sink);
        } catch (SAXException e) {
            // nothing
        }
        startStream();
        try {
            xmlReader.parse(new InputSource(source));
        } catch (SAXException e) {
            ParseException wrappedException = sink.processException(e);
            try {
                sink.endDocument();
            } catch (SAXException e2) {
                throw new ParseException(e2);
            }
            throw wrappedException;
        } catch (IOException e) {
            throw new ParseException(e);
        } finally {
            endStream();
        }
    }

    @Override
    public boolean setProperty(String key, Object value) {
        boolean sinkResult = super.setProperty(key, value);
        if (StreamProcessor.XML_READER_PROPERTY.equals(key) && value instanceof XMLReader) {
            xmlReader = (XMLReader) value;
        } else {
            return sinkResult;
        }
        return true;
    }

}
