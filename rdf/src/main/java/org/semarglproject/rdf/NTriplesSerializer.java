/**
 * Copyright 2012-2013 Lev Khomich
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

import org.semarglproject.sink.CharSink;
import org.semarglproject.sink.Pipe;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

/**
 * Implementation of {@link org.semarglproject.sink.TripleSink} which serializes triples to
 * {@link org.semarglproject.sink.CharSink} using <a href="">NTriples</a> syntax. *
 */
public final class NTriplesSerializer extends Pipe<CharSink> implements TripleSink {

    private static final String DOT_EOL = ".\n";

    private static final char QUOTE = '"';
    private static final char URI_START = '<';
    private static final char URI_END = '>';

    private static final char SPACE = ' ';

    private NTriplesSerializer(CharSink sink) {
        super(sink);
    }

    /**
     * Creates instance of TurtleSerializer connected to specified sink.
     * @param sink sink to be connected to
     * @return instance of TurtleSerializer
     */
    public static TripleSink connect(CharSink sink) {
        return new NTriplesSerializer(sink);
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        try {
            startTriple(subj, pred);
            if (obj.startsWith(RDF.BNODE_PREFIX)) {
                sink.process(obj);
            } else {
                serializeUri(obj);
            }
            sink.process(DOT_EOL);
        } catch (ParseException e) {
            // ignore
        }
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        try {
            startTriple(subj, pred);
            addContent(content);
            if (lang != null) {
                sink.process('@').process(lang);
            }
            sink.process(DOT_EOL);
        } catch (ParseException e) {
            // ignore
        }
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        try {
            startTriple(subj, pred);
            addContent(content);
            sink.process("^^");
            serializeUri(type);
            sink.process(DOT_EOL);
        } catch (ParseException e) {
            // ignore
        }
    }

    @Override
    protected boolean setPropertyInternal(String key, Object value) {
        return false;
    }

    @Override
    public void setBaseUri(String baseUri) {
        // ignore
    }

    private void startTriple(String subj, String pred) throws ParseException {
        if (subj.startsWith(RDF.BNODE_PREFIX)) {
            sink.process(subj).process(SPACE);
        } else {
            serializeUri(subj);
        }
        serializeUri(pred);
    }

    private void serializeUri(String uri) throws ParseException {
        String escapedUri = uri.replace("\\", "\\\\").replace(">", "\\u003E");
        sink.process(URI_START).process(escapedUri).process(URI_END).process(SPACE);
    }

    private void addContent(String content) throws ParseException {
        String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        sink.process(QUOTE).process(escapedContent).process(QUOTE);
    }

}
