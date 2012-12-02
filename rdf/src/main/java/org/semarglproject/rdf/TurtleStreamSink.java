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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public final class TurtleStreamSink implements TripleSink {

    private Writer writer;
    private OutputStream stream;

    private StringBuilder startTriple(String subj, String pred) {
        StringBuilder builder = new StringBuilder();
        if (subj.charAt(0) == '_') {
            builder.append(subj);
        } else {
            builder.append('<').append(subj).append('>');
        }
        builder.append(" <").append(pred).append("> ");
        return builder;
    }

    private void endTriple(StringBuilder builder) {
        builder.append(".\n");
        try {
            writer.write(builder.toString());
        } catch (IOException e) {
        }
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        StringBuilder builder = startTriple(subj, pred);
        if (obj.charAt(0) == '_') {
            builder.append(obj);
        } else {
            builder.append('<').append(obj).append('>');
        }
        endTriple(builder);
    }

    @Override
    public void addIriRef(String subj, String pred, String iri) {
        StringBuilder builder = startTriple(subj, pred);
        builder.append('<').append(iri).append('>');
        endTriple(builder);
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        StringBuilder builder = startTriple(subj, pred);
        content = content.replace("\"", "\\\"");
        if (content.contains("\n")) {
            builder.append("\"\"\"").append(content).append("\"\"\"");
        } else {
            builder.append('"').append(content).append('"');
        }
        if (lang != null) {
            builder.append('@').append(lang);
        }
        endTriple(builder);
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        StringBuilder builder = startTriple(subj, pred);
        content = content.replace("\"", "\\\"");
        if (content.contains("\n")) {
            builder.append("\"\"\"").append(content).append("\"\"\"");
        } else {
            builder.append('"').append(content).append('"');
        }
        builder.append("^^<").append(type).append('>');
        endTriple(builder);
    }

    @Override
    public void startStream() {
        if (stream == null) {
            throw new IllegalStateException("No stream specified");
        }
        writer = new OutputStreamWriter(stream);
    }

    @Override
    public void endStream() {
        try {
            writer.write("\n");
            writer.flush();
            writer.close();
            writer = null;
        } catch (IOException e) {
        }
    }

    @Override
    public void setBaseUri(String baseUri) {
    }

    public void setStream(OutputStream stream) {
        this.stream = stream;
    }
}
