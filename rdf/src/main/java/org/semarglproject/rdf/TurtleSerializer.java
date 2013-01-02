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

import org.semarglproject.sink.CharSink;
import org.semarglproject.sink.Converter;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Implementation of {@link TripleSink} which serializes triples to {@link CharSink} using
 * <a href="http://www.w3.org/TR/2012/WD-turtle-20120710/">Turtle</a> syntax. *
 */
public final class TurtleSerializer extends Converter<TripleSink, CharSink>  implements TripleSink {

    private static final String DOT_EOL = " .\n";
    private static final String COMMA_EOL = " ,\n";
    private static final String SEMICOLON_EOL = " ;\n";
    private static final String EOL = "\n";

    private static final String MULTILINE_QUOTE = "\"\"\"";
    private static final char SINGLE_LINE_QUOTE = '"';
    private static final char BNODE_START = '[';
    private static final char BNODE_END = ']';
    private static final char URI_START = '<';
    private static final char URI_END = '>';

    private static final char SPACE = ' ';
    private static final char RDF_TYPE_ABBR = 'a';
    private static final String INDENT = "    ";

    private static final short BATCH_SIZE = 10;

    private StringBuilder builder;

    private String prevSubj;
    private String prevPred;
    private short step;
    private final Queue<String> bnodeStack = new LinkedList<String>();
    private final Set<String> namedBnodes = new HashSet<String>();
    private String baseUri;

    private TurtleSerializer(CharSink sink) {
        super(sink);
    }

    /**
     * Creates instance of TurtleSerializer connected to specified sink.
     * @param sink sink to be connected to
     * @return instance of TurtleSerializer
     */
    public static TripleSink connect(CharSink sink) {
        return new TurtleSerializer(sink);
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        startTriple(subj, pred);
        if (obj.startsWith(RDF.BNODE_PREFIX)) {
            if (!namedBnodes.contains(obj) && obj.endsWith(RDF.SHORTENABLE_BNODE_SUFFIX)) {
                openBnode(obj);
            } else {
                builder.append(obj);
            }
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
        prevSubj = null;
        prevPred = null;
        builder = new StringBuilder();
        if (baseUri != null) {
            builder.append("@base ").append(URI_START).append(baseUri).append(URI_END).append(DOT_EOL);
        }
        builder.append("@prefix rdf: ").append(URI_START).append(RDF.NS).append(URI_END).append(DOT_EOL);
        step = 0;
        bnodeStack.clear();
        namedBnodes.clear();
        super.startStream();
    }

    @Override
    public void endStream() throws ParseException {
        super.endStream();
        if (builder == null) {
            builder = new StringBuilder();
        }
        while (!bnodeStack.isEmpty()) {
            closeBnode();
        }
        if (prevPred != null) {
            builder.append(DOT_EOL);
        } else {
            builder.append(EOL);
        }
        if (builder != null) {
            sink.process(builder.toString());
        }
        builder = null;
        baseUri = null;
    }

    @Override
    protected boolean setPropertyInternal(String key, Object value) {
        return false;
    }

    @Override
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri.substring(0, baseUri.length() - 1);
    }

    private void startTriple(String subj, String pred) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (subj.equals(prevSubj)) {
            if (pred.equals(prevPred)) {
                builder.append(COMMA_EOL);
                indent(2);
            } else if (prevPred != null) {
                builder.append(SEMICOLON_EOL);
                indent(1);
                serializePredicate(pred);
            } else {
                indent(0);
                serializePredicate(pred);
            }
        } else {
            if (!bnodeStack.isEmpty()) {
                closeBnode();
                startTriple(subj, pred);
                return;
            } else if (prevSubj != null) {
                builder.append(DOT_EOL);
            }
            if (subj.startsWith(RDF.BNODE_PREFIX)) {
                if (subj.endsWith(RDF.SHORTENABLE_BNODE_SUFFIX)) {
                    openBnode(subj);
                } else {
                    builder.append(subj).append(SPACE);
                    namedBnodes.add(subj);
                }
            } else {
                serializeUri(subj);
            }
            serializePredicate(pred);
        }
        prevSubj = subj;
        prevPred = pred;
    }

    private void serializePredicate(String pred) {
        if (RDF.TYPE.equals(pred)) {
            builder.append(RDF_TYPE_ABBR).append(SPACE);
        } else {
            serializeUri(pred);
        }
    }

    private void serializeUri(String uri) {
        if (uri.startsWith(RDF.NS)) {
            builder.append("rdf:").append(uri.substring(RDF.NS.length()));
        } else if (baseUri != null && uri.startsWith(baseUri)) {
            builder.append(URI_START).append(uri.substring(baseUri.length())).append(URI_END);
        } else {
            builder.append(URI_START).append(uri).append(URI_END);
        }
        builder.append(SPACE);
    }

    private void indent(int additionalIndent) {
        for (int i = 0; i < bnodeStack.size() + additionalIndent; i++) {
            builder.append(INDENT);
        }
    }

    private void endTriple() {
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
        content = content.replace("\\", "\\\\").replace("\"", "\\\"");
        if (content.contains(EOL)) {
            builder.append(MULTILINE_QUOTE).append(content).append(MULTILINE_QUOTE);
        } else {
            builder.append(SINGLE_LINE_QUOTE).append(content).append(SINGLE_LINE_QUOTE);
        }
    }

    private void openBnode(String obj) {
        builder.append(BNODE_START);
        bnodeStack.offer(obj);
        prevSubj = obj;
        prevPred = null;
    }

    private void closeBnode() {
        builder.append(BNODE_END);
        bnodeStack.poll();
        prevSubj = bnodeStack.peek();
        prevPred = null;
        if (prevSubj == null) {
            builder.append(DOT_EOL);
        }
    }

}
