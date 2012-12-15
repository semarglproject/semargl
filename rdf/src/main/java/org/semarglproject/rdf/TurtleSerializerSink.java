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

import org.semarglproject.vocab.RDF;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public final class TurtleSerializerSink implements TripleSink {

    private static final short BATCH_SIZE = 10;

    private Writer writer;
    private String prevSubj;
    private String prevPred;
    private StringBuilder builder;
    private short step;
    private Queue<String> bnodeStack = new LinkedList<String>();
    private Set<String> namedBnodes = new HashSet<String>();
    private String baseUri;

    private void startTriple(String subj, String pred) {
        String predicateStr;
        if (RDF.TYPE.equals(pred)) {
            predicateStr = " a ";
        } else if (pred.startsWith(RDF.NS)) {
            predicateStr = " rdf:" + pred.substring(RDF.NS.length()) + " ";
        } else {
            predicateStr = " <" + pred + "> ";
        }
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (subj.equals(prevSubj)) {
            if (pred.equals(prevPred)) {
                builder.append(", ");
            } else if (prevPred != null) {
                builder.append(";\n").append(predicateStr);
            } else {
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
                builder.append(subj);
                namedBnodes.add(subj);
            } else if (baseUri != null && subj.startsWith(baseUri)) {
                builder.append('<').append(subj.substring(baseUri.length())).append('>');
            } else {
                builder.append('<').append(subj).append('>');
            }
            builder.append(predicateStr);
        }
        prevSubj = subj;
        prevPred = pred;
    }

    private void endTriple() {
        if (step == BATCH_SIZE) {
            try {
                writer.write(builder.toString());
            } catch (IOException e) {
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
    public void startStream() {
        if (writer == null) {
            throw new IllegalStateException("No writer specified");
        }
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
    }

    @Override
    public void endStream() {
        baseUri = null;
        try {
            if (builder != null) {
                writer.write(builder.toString());
            }
            if (!bnodeStack.isEmpty()) {
                while (!bnodeStack.isEmpty()) {
                    writer.write("]");
                    bnodeStack.poll();
                }
                writer.write(" .\n");
            } else if (prevPred != null) {
                writer.write(" .");
            }
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
        }
    }

    @Override
    public void setBaseUri(String baseUri) {
        this.baseUri = //baseUri;
            baseUri.substring(0, baseUri.length() - 1);
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }
}
