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

package org.semarglproject.sink;

import org.semarglproject.rdf.ParseException;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public final class CharOutputSink implements CharSink {

    private File file;
    private Writer writer;
    private OutputStream outputStream;
    private boolean closeOnEndStream;

    public void setOutput(File file) {
        this.file = file;
        this.writer = null;
        this.outputStream = null;
        this.closeOnEndStream = true;
    }

    public void setOutput(Writer writer) {
        this.file = null;
        this.writer = writer;
        this.outputStream = null;
        this.closeOnEndStream = false;
    }

    public void setOutput(OutputStream outputStream) {
        this.file = null;
        this.writer = null;
        this.outputStream = outputStream;
        this.closeOnEndStream = false;
    }

    @Override
    public void process(String buffer) throws ParseException {
        try {
            writer.write(buffer);
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    @Override
    public void setBaseUri(String baseUri) {
    }

    @Override
    public void startStream() throws ParseException {
        if (writer == null) {
            if (file != null) {
                try {
                    writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
                } catch (FileNotFoundException e) {
                    new ParseException(e);
                }
            } else if (outputStream != null) {
                writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
            }
        }
    }

    @Override
    public void endStream() throws ParseException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new ParseException(e);
        }
        if (closeOnEndStream) {
            if (writer != null) {
                closeQuietly(writer);
                writer = null;
            } else if (outputStream != null) {
                closeQuietly(outputStream);
                outputStream = null;
            }
        }
    }

    @Override
    public boolean setProperty(String key, Object value) {
        return false;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }
}
