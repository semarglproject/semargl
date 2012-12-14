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

import org.semarglproject.xml.XmlUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.BitSet;

public final class NTriplesParser implements CharSink, TripleSource {

    private static final short MODE_SAVE_UNTIL = 1;
    private static final short MODE_SAVE_WHILE = 2;

    private String buffer = null;
    private TripleSink sink = null;
    private int pos = -1;
    private int limit = -1;

    private static void error(String msg) throws ParseException {
        throw new ParseException(msg);
    }

    @Override
    public void read(Reader r) throws ParseException {
        BufferedReader reader = new BufferedReader(r);
        try {
            while ((buffer = reader.readLine()) != null) {
                if (isEntirelyWhitespaceOrEmpty(buffer)) {
                    continue;
                }
                parseLine();
            }
        } catch (IOException e) {
            throw new ParseException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // nothing
            }
        }
    }

    private static boolean isEntirelyWhitespaceOrEmpty(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    private void skipWhitespace() {
        while (pos < limit && XmlUtils.WHITESPACE.get(buffer.charAt(pos))) {
            pos++;
        }
    }

    private void parseLine() throws ParseException {
        pos = 0;
        limit = buffer.length();

        String subj = null;
        String pred = null;

        outer:
        for (; pos < limit; pos++) {
            skipWhitespace();

            String value;
            switch (buffer.charAt(pos)) {
                case '<':
                    pos++;
                    value = getToken(MODE_SAVE_UNTIL, XmlUtils.GT);
                    if (subj == null) {
                        subj = value;
                    } else if (pred == null) {
                        pred = value;
                    } else {
                        sink.addIriRef(subj, pred, value);
                        break outer;
                    }
                    break;
                case '_':
                    value = getToken(MODE_SAVE_WHILE, XmlUtils.ID);
                    if (subj == null) {
                        subj = value;
                    } else if (pred == null) {
                        error("Predicate specified by bnode");
                    } else {
                        sink.addNonLiteral(subj, pred, value);
                        break outer;
                    }
                    break;
                case '"':
                    pos++;
                    value = unescape(getToken(MODE_SAVE_UNTIL, XmlUtils.QUOTE));
                    while (buffer.charAt(pos - 2) == '\\') {
                        value += '"' + unescape(getToken(MODE_SAVE_UNTIL, XmlUtils.QUOTE));
                    }
                    if (subj == null || pred == null) {
                        error("Literal before subject or predicate");
                    }
                    if (pos + 2 >= limit - 1) {
                        sink.addPlainLiteral(subj, pred, value, null);
                        break outer;
                    }
                    if (buffer.charAt(pos) == '^' && buffer.charAt(pos + 1) == '^'
                            && buffer.charAt(pos + 2) == '<') {
                        pos += 3;
                        String type = getToken(MODE_SAVE_UNTIL, XmlUtils.GT);
                        sink.addTypedLiteral(subj, pred, value, type);
                        break outer;
                    }
                    if (buffer.charAt(pos) == '@') {
                        pos++;
                        String lang = getToken(MODE_SAVE_UNTIL, XmlUtils.WHITESPACE);
                        sink.addPlainLiteral(subj, pred, value, lang);
                        break outer;
                    }
                    sink.addPlainLiteral(subj, pred, value, null);
                    break outer;
                case '#':
                    // if (subj != null) {
                    // error("Error parsing triple");
                    // }
                    return;
                default:
                    error("Error parsing triple");
            }
        }
        skipWhitespace();
        if (pos != limit && buffer.charAt(pos) != '#' && buffer.charAt(pos) != '.') {
            error("Error parsing triple");
        }
    }

    private String getToken(short mode, BitSet checker) {
        int savedLength = 0;
        int startPos = pos;
        loop:
        for (; pos < limit; pos++) {
            switch (mode) {
                case MODE_SAVE_WHILE:
                    if (!checker.get(buffer.charAt(pos))) {
                        break loop;
                    }
                    savedLength++;
                    if (pos == limit - 1) {
                        break loop;
                    }
                    break;
                case MODE_SAVE_UNTIL:
                    if (checker.get(buffer.charAt(pos))) {
                        pos++;
                        break loop;
                    }
                    savedLength++;
                    if (pos == limit - 1) {
                        pos++;
                        break loop;
                    }
                    break;
            }
        }
        return buffer.substring(startPos, startPos + savedLength);
    }

    private static String unescape(String str) throws ParseException {
        int limit = str.length();
        StringBuilder result = new StringBuilder(limit);

        for (int i = 0; i < limit; i++) {
            char ch = str.charAt(i);
            if (ch != '\\') {
                result.append(ch);
                continue;
            }
            i++;
            if (i == limit) {
                break;
            }
            ch = str.charAt(i);
            switch (ch) {
                case '\\':
                case '\'':
                case '\"':
                    result.append(ch);
                    break;
                case 'b':
                    result.append('\b');
                    break;
                case 'f':
                    result.append('\f');
                    break;
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'u':
                    if (i + 4 >= limit) {
                        error("Error parsing escaped char");
                    }
                    String code = str.substring(i + 1, i + 5);
                    i += 4;
                    try {
                        int value = Integer.parseInt(code, 16);
                        result.append((char) value);
                    } catch (NumberFormatException nfe) {
                        error("Error parsing escaped char");
                    }
                    break;
                default:
                    result.append(ch);
                    break;
            }
        }
        return result.toString();
    }

    @Override
    public void setBaseUri(String baseUri) {
    }

    public NTriplesParser streamingTo(TripleSink sink) {
        this.sink = sink;
        return this;
    }

    @Override
    public void startStream() {
        sink.startStream();
    }

    @Override
    public void endStream() {
        sink.endStream();
    }

}
