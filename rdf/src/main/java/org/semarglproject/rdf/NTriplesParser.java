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
import org.semarglproject.xml.XmlUtils;

import java.util.BitSet;

/**
 * Implementation of streaming <a href="http://www.w3.org/2001/sw/RDFCore/ntriples/">NTriples</a> parser.
 */
public final class NTriplesParser extends Converter<CharSink, TripleSink> implements CharSink {

    private static final short MODE_SAVE_UNTIL = 1;
    private static final short MODE_SAVE_WHILE = 2;

    private String subj = null;
    private String pred = null;

    private String buffer = null;
    private int pos = -1;
    private int limit = -1;

    private NTriplesParser(TripleSink sink) {
        super(sink);
    }

    /**
     * Creates instance of NTriplesParser connected to specified sink.
     * @param sink sink to be connected to
     * @return instance of NTriplesParser
     */
    public static CharSink connect(TripleSink sink) {
        return new NTriplesParser(sink);
    }

    private static void error(String msg) throws ParseException {
        throw new ParseException(msg);
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

    @Override
    public void process(String line) throws ParseException {
        if (isEntirelyWhitespaceOrEmpty(line)) {
            return;
        }
        this.buffer = line;

        pos = 0;
        limit = line.length();

        subj = null;
        pred = null;

        boolean nextLine = false;

        for (; pos < limit && !nextLine; pos++) {
            skipWhitespace();

            String value;
            switch (line.charAt(pos)) {
                case '<':
                    pos++;
                    value = unescape(getToken(MODE_SAVE_UNTIL, XmlUtils.GT));
                    nextLine = processNonLiteral(value);
                    break;
                case '_':
                    value = unescape(getToken(MODE_SAVE_WHILE, XmlUtils.ID));
                    nextLine = processNonLiteral(value);
                    break;
                case '"':
                    pos++;
                    value = unescape(getToken(MODE_SAVE_UNTIL, XmlUtils.QUOTE));
                    while (line.charAt(pos - 2) == '\\') {
                        value += '"' + unescape(getToken(MODE_SAVE_UNTIL, XmlUtils.QUOTE));
                    }
                    if (subj == null || pred == null) {
                        error("Literal before subject or predicate");
                    }
                    parseLiteral(subj, pred, value);
                    nextLine = true;
                    break;
                case '#':
                    return;
                default:
                    error("Unknown token '" + line.charAt(pos) + "' in line '" + line + "'");
            }
        }
        skipWhitespace();
        if (pos != limit && line.charAt(pos) != '#' && line.charAt(pos) != '.') {
            error("Error parsing triple");
        }
    }

    private boolean processNonLiteral(String value) {
        boolean nextLine = false;
        if (subj == null) {
            subj = value;
        } else if (pred == null) {
            pred = value;
        } else {
            sink.addNonLiteral(subj, pred, value);
            nextLine = true;
        }
        return nextLine;
    }

    private void parseLiteral(String subj, String pred, String value) {
        if (pos + 2 >= limit - 1) {
            sink.addPlainLiteral(subj, pred, value, null);
        } else if (buffer.charAt(pos) == '^' && buffer.charAt(pos + 1) == '^'
                && buffer.charAt(pos + 2) == '<') {
            pos += 3;
            String type = getToken(MODE_SAVE_UNTIL, XmlUtils.GT);
            sink.addTypedLiteral(subj, pred, value, type);
        } else if (buffer.charAt(pos) == '@') {
            pos++;
            String lang = getToken(MODE_SAVE_UNTIL, XmlUtils.WHITESPACE);
            sink.addPlainLiteral(subj, pred, value, lang);
        } else {
            sink.addPlainLiteral(subj, pred, value, null);
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
                default:
                    throw new IllegalStateException("Unknown mode = " + mode);
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

    @Override
    protected boolean setPropertyInternal(String key, Object value) {
        return false;
    }
}
