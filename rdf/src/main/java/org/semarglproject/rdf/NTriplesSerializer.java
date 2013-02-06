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

    private static final short BATCH_SIZE = 10;

    private StringBuilder builder;
    private short step;

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
        startTriple(subj, pred);
        if (obj.startsWith(RDF.BNODE_PREFIX)) {
            builder.append(obj);
        } else {
            serializeUri(obj);
        }
        endTriple();
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        startTriple(subj, pred);
        addContent(content);
        if (lang != null) {
            builder.append('@').append(lang);
        }
        endTriple();
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        startTriple(subj, pred);
        addContent(content);
        builder.append("^^");
        serializeUri(type);
        endTriple();
    }

    @Override
    public void startStream() throws ParseException {
        builder = new StringBuilder();
        step = 0;
        super.startStream();
    }

    @Override
    public void endStream() throws ParseException {
        super.endStream();
        if (builder != null) {
            sink.process(builder.toString());
        }
        builder = null;
    }

    @Override
    protected boolean setPropertyInternal(String key, Object value) {
        return false;
    }

    @Override
    public void setBaseUri(String baseUri) {
        // ignore
    }

    private void startTriple(String subj, String pred) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (subj.startsWith(RDF.BNODE_PREFIX)) {
            builder.append(subj).append(SPACE);
        } else {
            serializeUri(subj);
        }
        serializeUri(pred);
    }

    private void serializeUri(String uri) {
        String escapedUri = uri.replace("\\", "\\\\").replace(">", "\\u003E");
        builder.append(URI_START).append(escapedUri).append(URI_END).append(SPACE);
    }

    private void endTriple() {
        builder.append(DOT_EOL);
        if (step == BATCH_SIZE) {
            try {
                sink.process(builder.toString());
            } catch (ParseException e) {
                // do nothing
            }
            builder = null;
            step = 0;
        }
        step++;
    }

    private void addContent(String content) {
        String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        builder.append(QUOTE).append(escapedContent).append(QUOTE);
    }

}
