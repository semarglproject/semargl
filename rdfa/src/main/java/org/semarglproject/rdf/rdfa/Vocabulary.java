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

package org.semarglproject.rdf.rdfa;

import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.RdfXmlParser;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.ri.IRI;
import org.semarglproject.vocab.RDF;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Vocabulary {
    String url;
    private Map<String, Collection<String>> expansions;
    private Collection<String> terms;

    public Vocabulary(String url) {
        this.url = url;
    }

    private void addExpansion(String pred, String expansion) {
        if (!expansions.containsKey(pred)) {
            expansions.put(pred, new HashSet<String>());
        }
        expansions.get(pred).add(expansion);
    }

    public void load() {
        VocabParser vocabParser = new VocabParser();

        XMLReader xmlReader;
        URL vocabUrl;
        try {
            xmlReader = XMLReaderFactory.createXMLReader();
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            vocabUrl = new URL(url);
        } catch (MalformedURLException e) {
            return;
        } catch (SAXException e) {
            return;
        }

        if (expansions == null) {
            expansions = new HashMap<String, Collection<String>>();
            terms = new HashSet<String>();
        }

        DataProcessor<Reader> rdfaDp = new SaxSource(xmlReader).streamingTo(
                new RdfaParser(true, true, false).streamingTo(vocabParser)).build();
        parseVocabWithDp(vocabUrl, xmlReader, rdfaDp);

        if (!terms.isEmpty() || !expansions.isEmpty()) {
            return;
        }

        // TODO: add format detection
        DataProcessor<Reader> rdfXmlDp = new SaxSource(xmlReader).streamingTo(
                new RdfXmlParser().streamingTo(vocabParser)).build();
        parseVocabWithDp(vocabUrl, xmlReader, rdfXmlDp);

        if (terms.isEmpty() && expansions.isEmpty()) {
            terms = null;
            expansions = null;
        }
    }

    private void parseVocabWithDp(URL vocabUrl, XMLReader xmlReader, DataProcessor<Reader> dp) {
        InputStream inputStream;
        try {
            inputStream = vocabUrl.openStream();
        } catch (IOException e) {
            return;
        }
        InputStreamReader reader = new InputStreamReader(inputStream);
        try {
            dp.process(reader, url);
        } catch (ParseException e) {
            // do nothing
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public Collection<String> expand(String uri) {
        if (expansions == null || !expansions.containsKey(uri)) {
            return Collections.EMPTY_LIST;
        }
        return expansions.get(uri);
    }

    public String resolveTerm(String term) {
        String termUri = url + term;
        if (terms == null && IRI.isAbsolute(termUri) || terms != null && terms.contains(termUri)) {
            return termUri;
        }
        return null;
    }

    private final class VocabParser implements TripleSink {
        @Override
        public void addNonLiteral(String subj, String pred, String obj) {
            if (subj.startsWith(RDF.BNODE_PREFIX) || obj.startsWith(RDF.BNODE_PREFIX)) {
                return;
            }
            if (pred.equals(OWL.EQUIVALENT_PROPERTY) || pred.equals(OWL.EQUIVALENT_CLASS)) {
                addExpansion(subj, obj);
                addExpansion(obj, subj);
                terms.add(obj);
                terms.add(subj);
            } else if (pred.equals(RDFS.SUB_CLASS_OF) || pred.equals(RDFS.SUB_PROPERTY_OF)) {
                addExpansion(subj, obj);
                terms.add(obj);
                terms.add(subj);
            }
            if (pred.equals(RDF.TYPE) && (obj.equals(RDF.PROPERTY) || obj.equals(RDFS.CLASS))) {
                terms.add(subj);
            }
        }

        @Override
        public void addPlainLiteral(String subj, String pred, String content, String lang) {
        }

        @Override
        public void addTypedLiteral(String subj, String pred, String content, String type) {
        }

        @Override
        public void setBaseUri(String baseUri) {
        }

        @Override
        public void startStream() {
        }

        @Override
        public void endStream() {
        }
    }
}
