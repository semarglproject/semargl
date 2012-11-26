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

package org.semarglproject.ri;

import java.util.HashMap;
import java.util.Map;

public final class CURIE {

    private static final String XHTML_VOCAB = "http://www.w3.org/1999/xhtml/vocab#";
    private static final String POWDER_VOCAB = "http://www.w3.org/2007/05/powder-s#";

    private static final Map<String, String> KNOWN_PREFIXES = new HashMap<String, String>();

    static {
        KNOWN_PREFIXES.put("dc", "http://purl.org/dc/terms/");
        KNOWN_PREFIXES.put("owl", "http://www.w3.org/2002/07/owl#");
        KNOWN_PREFIXES.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        KNOWN_PREFIXES.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        KNOWN_PREFIXES.put("rdfa", "http://www.w3.org/ns/rdfa#");
        KNOWN_PREFIXES.put("foaf", "http://xmlns.com/foaf/0.1/");
        KNOWN_PREFIXES.put("xhv", XHTML_VOCAB);
        KNOWN_PREFIXES.put("xsd", "http://www.w3.org/2001/XMLSchema#");

        KNOWN_PREFIXES.put("grddl", "http://www.w3.org/2003/g/data-view#");
        KNOWN_PREFIXES.put("ma", "http://www.w3.org/ns/ma-ont#");
        KNOWN_PREFIXES.put("rif", "http://www.w3.org/2007/rif#");
        KNOWN_PREFIXES.put("skos", "http://www.w3.org/2004/02/skos/core#");
        KNOWN_PREFIXES.put("skosxl", "http://www.w3.org/2008/05/skos-xl#");
        KNOWN_PREFIXES.put("wdr", "http://www.w3.org/2007/05/powder#");
        KNOWN_PREFIXES.put("void", "http://rdfs.org/ns/void#");
        KNOWN_PREFIXES.put("wdrs", "http://www.w3.org/2007/05/powder-s#");
        KNOWN_PREFIXES.put("xml", "http://www.w3.org/XML/1998/namespace");
        KNOWN_PREFIXES.put("cc", "http://creativecommons.org/ns#");
        KNOWN_PREFIXES.put("ctag", "http://commontag.org/ns#");
        KNOWN_PREFIXES.put("dcterms", "http://purl.org/dc/terms/");
        KNOWN_PREFIXES.put("gr", "http://purl.org/goodrelations/v1#");
        KNOWN_PREFIXES.put("ical", "http://www.w3.org/2002/12/cal/icaltzd#");
        KNOWN_PREFIXES.put("og", "http://ogp.me/ns#");
        KNOWN_PREFIXES.put("rev", "http://purl.org/stuff/rev#");
        KNOWN_PREFIXES.put("sioc", "http://rdfs.org/sioc/ns#");
        KNOWN_PREFIXES.put("v", "http://rdf.data-vocabulary.org/#");
        KNOWN_PREFIXES.put("vcard", "http://www.w3.org/2006/vcard/ns#");
        KNOWN_PREFIXES.put("schema", "http://schema.org/");
    }

    private static final String[] XHTML_VOCAB_PROPS = {"alternate", "appendix", "bookmark",
            "cite", "chapter", "contents", "copyright", "first", "glossary", "help", "icon",
            "index", "itsRules", "last", "license", "meta", "next", "p3pv1", "prev", "previous",
            "role", "section", "stylesheet", "subsection", "start", "top", "up"};
    private static final String[] POWDER_VOCAB_PROPS = {"text", "issuedby", "matchesregex",
            "notmatchesregex", "hasIRI", "tag", "notknownto", "describedby", "authenticate",
            "validfrom", "validuntil", "logo", "sha1sum", "certified", "certifiedby",
            "supportedby", "data_error", "proc_error", "error_code"};

    public static String resolveXhtmlVocabLink(String predicate) {
        for (String link : XHTML_VOCAB_PROPS) {
            if (link.equalsIgnoreCase(predicate)) {
                return XHTML_VOCAB + link;
            }
        }
        return null;
    }

    public static String resolvePowderVocabLink(String predicate) {
        for (String link : POWDER_VOCAB_PROPS) {
            if (link.equalsIgnoreCase(predicate)) {
                return POWDER_VOCAB + link;
            }
        }
        return null;
    }

    public static String resolve(String curie, Map<String, String> iriMappings,
                                 boolean resolveKnownPrefixes) throws MalformedCURIEException {
        if (curie == null || curie.isEmpty()) {
            return null;
//            throw new MalformedCURIEException("Empty CURIE");
        }
        boolean safeSyntax = curie.charAt(0) == '[' && curie.charAt(curie.length() - 1) == ']';
        if (safeSyntax) {
            curie = curie.substring(1, curie.length() - 1);
        }

        int delimPos = curie.indexOf(':');
        if (delimPos == -1) {
            return null;
//            throw new MalformedCURIEException("CURIE with no prefix");
        }
        String localName = curie.substring(delimPos + 1);
        String prefix = curie.substring(0, delimPos);

        if (prefix.equals("_")) {
            throw new MalformedCURIEException("CURIE with '_' prefix");
        }
        // TODO: check for correct prefix
        if (!iriMappings.containsKey(prefix)) {
            if (!resolveKnownPrefixes || !KNOWN_PREFIXES.containsKey(prefix)) {
                throw new MalformedCURIEException("Unknown prefix");
//                return null;
            } else {
                return KNOWN_PREFIXES.get(prefix) + localName;
            }
        }
        return iriMappings.get(prefix) + localName;
    }

    private CURIE() {
    }

    // public static String resolveSafe(String curie, Map<String, String>
    // iriMappings,
    // boolean searchForKnownPrefixes) throws MalformedCURIEException {
    // if (curie == null || curie.isEmpty()) {
    // throw new MalformedCURIEException("Empty CURIE");
    // }
    // boolean safeSyntax = curie.charAt(0) == '[' &&
    // curie.charAt(curie.length() - 1) == ']';
    // if (!safeSyntax)
    // return null;
    // return resolve(curie, iriMappings, searchForKnownPrefixes);
    // }
}
