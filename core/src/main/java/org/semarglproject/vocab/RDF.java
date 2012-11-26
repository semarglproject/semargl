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

package org.semarglproject.vocab;

public final class RDF {

    public static final String BNODE_PREFIX = "_:";

    private RDF() {
    }

    public static final String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static final String ABOUT = NS + "about";
    public static final String ALT = NS + "Alt";
    public static final String BAG = NS + "Bag";
    public static final String DATATYPE = NS + "datatype";
    public static final String DESCRIPTION = NS + "Description";
    public static final String FIRST = NS + "first";
    public static final String ID = NS + "ID";
    public static final String LI = NS + "li";
    public static final String LIST = NS + "List";
    public static final String NIL = NS + "nil";
    public static final String PLAIN_LITERAL = NS + "PlainLiteral";
    public static final String PREDICATE = NS + "predicate";
    public static final String PROPERTY = NS + "Property";
    public static final String NODEID = NS + "nodeID";
    public static final String OBJECT = NS + "object";
    public static final String PARSE_TYPE = NS + "parseType";
    public static final String RDF = NS + "RDF";
    public static final String RESOURCE = NS + "resource";
    public static final String REST = NS + "rest";
    public static final String SEQ = NS + "Seq";
    public static final String STATEMENT = NS + "Statement";
    public static final String SUBJECT = NS + "subject";
    public static final String TYPE = NS + "type";
    public static final String VALUE = NS + "value";
    public static final String XML_LITERAL = NS + "XMLLiteral";

    @Deprecated
    public static final String ABOUT_EACH = NS + "aboutEach";
    @Deprecated
    public static final String ABOUT_EACH_PREFIX = NS + "aboutEachPrefix";
    @Deprecated
    public static final String BAG_ID = NS + "bagID";
}
