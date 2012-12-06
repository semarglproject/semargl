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

import java.io.IOException;
import java.io.Writer;

public final class TurtleSerializerSink implements TripleSink {

    private static final short BATCH_SIZE = 10;

    private Writer writer;
    private String prevSubj;
    private String prevPred;
    private StringBuilder builder;
    private short step;

    private void startTriple(String subj, String pred) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        if (subj.equals(prevSubj)) {
            if (pred.equals(prevPred)) {
                builder.append(", ");
            } else {
                builder.append(";\n <").append(pred).append("> ");
            }
        } else {
            if (prevSubj != null) {
                builder.append(".\n");
            }
            if (subj.charAt(0) == '_') {
                builder.append(subj);
            } else {
                builder.append('<').append(subj).append('>');
            }
            builder.append(" <").append(pred).append("> ");
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
            builder.append(obj);
        } else {
            builder.append('<').append(obj).append('>');
        }
        endTriple();
    }

    @Override
    public void addIriRef(String subj, String pred, String iri) {
        startTriple(subj, pred);
        builder.append('<').append(iri).append('>');
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
        builder = null;
        step = 0;
    }

    @Override
    public void endStream() {
        try {
            if (builder != null) {
                writer.write(builder.toString());
            }
            if (prevPred != null) {
                writer.write('.');
            }
            writer.write('\n');
            writer.flush();
        } catch (IOException e) {
        }
    }

    @Override
    public void setBaseUri(String baseUri) {
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }
}
