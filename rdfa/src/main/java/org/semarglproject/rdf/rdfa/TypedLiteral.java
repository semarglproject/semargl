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

package org.semarglproject.rdf.rdfa;

import org.semarglproject.rdf.ParseException;
import org.semarglproject.vocab.XSD;

import javax.xml.bind.DatatypeConverter;

final class TypedLiteral implements LiteralNode {
    private final String content;
    private final String type;

    TypedLiteral(String content, String dt) {
        super();
        this.content = content;
        this.type = dt;
    }

    @Override
    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public static TypedLiteral from(String content, String dt) throws ParseException {
        if (dt.equals(RdfaParser.AUTODETECT_DATE_DATATYPE)) {
            try {
                if (content.matches("-?P\\d+Y\\d+M\\d+DT\\d+H\\d+M\\d+(\\.\\d+)?S")) {
                    return new TypedLiteral(content, XSD.DURATION);
                }
                if (content.indexOf(':') != -1) {
                    if (content.indexOf('T') != -1) {
                        DatatypeConverter.parseDateTime(content);
                        return new TypedLiteral(content, XSD.DATE_TIME);
                    }
                    DatatypeConverter.parseTime(content);
                    return new TypedLiteral(content, XSD.TIME);
                }
                if (content.matches("-?\\d{4,}")) {
                    return new TypedLiteral(content, XSD.G_YEAR);
                }
                if (content.matches("-?\\d{4,}-(0[1-9]|1[0-2])")) {
                    return new TypedLiteral(content, XSD.G_YEAR_MONTH);
                }
                DatatypeConverter.parseDate(content);
                return new TypedLiteral(content, XSD.DATE);
            } catch (IllegalArgumentException e) {
                throw new ParseException("Ill-formed typed literal '" + content + "'^^<" + dt + ">");
            }
        }
        return new TypedLiteral(content, dt);
    }

}
