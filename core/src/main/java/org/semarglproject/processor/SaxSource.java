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
import org.semarglproject.sink.SaxSink;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.Reader;

public final class SaxSource extends DataProcessor<Reader, SaxSink> {

    public static final String XML_PARSER_PROPERTY = "http://semarglproject.org/core/properties/xml-parser";

    private XMLReader xmlReader = null;

    private SaxSource(SaxSink sink) {
        super(sink);
    }

    @Override
    public void process(Reader source) throws ParseException {
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
        super.startStream();
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
            super.endStream();
        }

    }

    public static StreamProcessor<Reader> streamingTo(SaxSink sink) {
        return new SaxSource(sink);
    }

    @Override
    public boolean setProperty(String key, Object value) {
        boolean sinkResult = super.setProperty(key, value);
        if (XML_PARSER_PROPERTY.equals(key) && value instanceof XMLReader) {
            xmlReader = (XMLReader) value;
        } else {
            return sinkResult;
        }
        return true;
    }
}
