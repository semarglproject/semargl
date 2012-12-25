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
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

// TODO: improve readability
public final class TurtleSerializer implements TripleSink {

    private static final short BATCH_SIZE = 10;

    private CharSink sink;

    private String prevSubj;
    private String prevPred;
    private StringBuilder builder;
    private short step;
    private Queue<String> bnodeStack = new LinkedList<String>();
    private Set<String> namedBnodes = new HashSet<String>();
    private String baseUri;

    private TurtleSerializer(CharSink sink) {
        this.sink = sink;
    }

    public static TripleSink connect(CharSink sink) {
        return new TurtleSerializer(sink);
    }

    private void startTriple(String subj, String pred) {
        String predicateStr;
        if (RDF.TYPE.equals(pred)) {
            predicateStr = "a ";
        } else if (pred.startsWith(RDF.NS)) {
            predicateStr = "rdf:" + pred.substring(RDF.NS.length()) + " ";
        } else {
            predicateStr = "<" + pred + "> ";
        }
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (subj.equals(prevSubj)) {
            if (pred.equals(prevPred)) {
                builder.append(",\n");
                indent(2);
            } else if (prevPred != null) {
                builder.append(";\n");
                indent(1);
                builder.append(predicateStr);
            } else {
                indent(0);
                builder.append(predicateStr);
            }
        } else {
            if (!bnodeStack.isEmpty()) {
                builder.append("]");
                bnodeStack.poll();
                prevSubj = bnodeStack.peek();
                prevPred = null;
                if (prevSubj == null) {
                    builder.append(" .\n");
                }
                startTriple(subj, pred);
                return;
            } else if (prevSubj != null) {
                builder.append(" .\n");
            }
            if (subj.charAt(0) == '_') {
                if (subj.endsWith(RDF.SHORTENABLE_BNODE_SUFFIX)) {
                    builder.append('[');
                    bnodeStack.offer(subj);
                } else {
                    builder.append(subj).append(' ');
                    namedBnodes.add(subj);
                }
            } else if (baseUri != null && subj.startsWith(baseUri)) {
                builder.append('<').append(subj.substring(baseUri.length())).append("> ");
            } else {
                builder.append('<').append(subj).append("> ");
            }
            builder.append(predicateStr);
        }
        prevSubj = subj;
        prevPred = pred;
    }

    private void indent(int additionalIndent) {
        for (int i = 0; i < bnodeStack.size() + additionalIndent; i++) {
            builder.append('\t');
        }
    }

    private void endTriple() {
        if (step == BATCH_SIZE) {
            try {
                sink.process(builder.toString());
            } catch (ParseException e) {

            }
            builder = null;
            step = 0;
        }
        step++;
    }

    private void addContent(String content) {
        content = content.replace("\"", "\\\"");
        if (content.contains("\n")) {
            builder.append("\"\"\"").append(content).append("\"\"\"");
        } else {
            builder.append('"').append(content).append('"');
        }
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        startTriple(subj, pred);
        if (obj.charAt(0) == '_') {
            if (!namedBnodes.contains(obj) && obj.endsWith(RDF.SHORTENABLE_BNODE_SUFFIX)) {
                builder.append('[');
                bnodeStack.offer(obj);
                prevSubj = obj;
                prevPred = null;
            } else {
                builder.append(obj);
            }
        } else {
            if (obj.startsWith(RDF.NS)) {
                builder.append("rdf:").append(obj.substring(RDF.NS.length()));
            } else if (baseUri != null && obj.startsWith(baseUri)) {
                builder.append('<').append(obj.substring(baseUri.length())).append('>');
            } else {
                builder.append('<').append(obj).append('>');
            }
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
        builder.append("^^<").append(type).append('>');
        endTriple();
    }

    @Override
    public void startStream() throws ParseException {
        prevSubj = null;
        prevPred = null;
        builder = new StringBuilder();
        if (baseUri != null) {
            builder.append("@base <").append(baseUri).append("> .\n");
        }
        builder.append("@prefix rdf: <").append(RDF.NS).append("> .\n");
        step = 0;
        bnodeStack.clear();
        namedBnodes.clear();
        sink.startStream();
    }

    @Override
    public void endStream() throws ParseException {
        sink.endStream();
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (!bnodeStack.isEmpty()) {
            while (!bnodeStack.isEmpty()) {
                builder.append("]");
                bnodeStack.poll();
            }
            builder.append(" .\n");
        } else if (prevPred != null) {
            builder.append(" .");
        }
        builder.append('\n');
        if (builder != null) {
            sink.process(builder.toString());
        }
        builder = null;
        baseUri = null;
    }

    @Override
    public boolean setProperty(String key, Object value) {
        return false;
    }

    @Override
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri.substring(0, baseUri.length() - 1);
    }
}
