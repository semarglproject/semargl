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

    // Initial context described in http://www.w3.org/2011/rdfa-context/rdfa-1.1.html
    public static final Map<String, String> INITIAL_CONTEXT = new HashMap<String, String>();

    static {
        // Vocabulary Prefixes of W3C Documents
        INITIAL_CONTEXT.put("owl", "http://www.w3.org/2002/07/owl#");
        INITIAL_CONTEXT.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        INITIAL_CONTEXT.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        INITIAL_CONTEXT.put("rdfa", "http://www.w3.org/ns/rdfa#");
        INITIAL_CONTEXT.put("xhv", XHTML_VOCAB);
        INITIAL_CONTEXT.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        INITIAL_CONTEXT.put("grddl", "http://www.w3.org/2003/g/data-view#");
        INITIAL_CONTEXT.put("ma", "http://www.w3.org/ns/ma-ont#");
        INITIAL_CONTEXT.put("rif", "http://www.w3.org/2007/rif#");
        INITIAL_CONTEXT.put("skos", "http://www.w3.org/2004/02/skos/core#");
        INITIAL_CONTEXT.put("skosxl", "http://www.w3.org/2008/05/skos-xl#");
        INITIAL_CONTEXT.put("wdr", "http://www.w3.org/2007/05/powder#");
        INITIAL_CONTEXT.put("void", "http://rdfs.org/ns/void#");
        INITIAL_CONTEXT.put("wdrs", "http://www.w3.org/2007/05/powder-s#");
        INITIAL_CONTEXT.put("xml", "http://www.w3.org/XML/1998/namespace");

        // Widely used Vocabulary prefixes
        INITIAL_CONTEXT.put("cc", "http://creativecommons.org/ns#");
        INITIAL_CONTEXT.put("ctag", "http://commontag.org/ns#");
        INITIAL_CONTEXT.put("dc", "http://purl.org/dc/terms/");
        INITIAL_CONTEXT.put("dcterms", "http://purl.org/dc/terms/");
        INITIAL_CONTEXT.put("foaf", "http://xmlns.com/foaf/0.1/");
        INITIAL_CONTEXT.put("gr", "http://purl.org/goodrelations/v1#");
        INITIAL_CONTEXT.put("ical", "http://www.w3.org/2002/12/cal/icaltzd#");
        INITIAL_CONTEXT.put("og", "http://ogp.me/ns#");
        INITIAL_CONTEXT.put("rev", "http://purl.org/stuff/rev#");
        INITIAL_CONTEXT.put("sioc", "http://rdfs.org/sioc/ns#");
        INITIAL_CONTEXT.put("v", "http://rdf.data-vocabulary.org/#");
        INITIAL_CONTEXT.put("vcard", "http://www.w3.org/2006/vcard/ns#");
        INITIAL_CONTEXT.put("schema", "http://schema.org/");
    }

    private static final String[] XHTML_VOCAB_PROPS = {
        // XHTML Metainformation Vocabulary
        "alternate", "appendix", "bookmark", "cite", "chapter", "contents",
        "copyright", "first", "glossary", "help", "icon", "index", "itsRules",
        "last", "license", "meta", "next", "p3pv1", "prev", "previous", "role",
        "section", "stylesheet", "subsection", "start","top", "up",

        // Items from the XHTML Role Module
        "banner", "complementary", "contentinfo", "definition", "main",
        "navigation", "note", "search",

        // Items from the Accessible Rich Internet Applications Vocabulary
        "alert", "alertdialog", "application", "article", "button", "checkbox",
        "columnheader", "combobox", "dialog", "directory", "document", "form",
        "grid", "gridcell", "group", "heading", "img", "link", "list", "listbox",
        "listitem", "log", "marquee", "math", "menu", "menubar", "menuitem",
        "menuitemcheckbox", "menuitemradio", "option", "presentation",
        "progressbar", "radio", "radiogroup", "region", "row", "rowgroup",
        "rowheader", "scrollbar", "separator", "slider", "spinbutton", "status",
        "tab", "tablist", "tabpanel", "textbox", "timer", "toolbar", "tooltip",
        "tree", "treegrid", "treeitem"
    };

    private static final String[] POWDER_VOCAB_PROPS = {"text", "issuedby", "matchesregex",
            "notmatchesregex", "hasIRI", "tag", "notknownto", "describedby", "authenticate",
            "validfrom", "validuntil", "logo", "sha1sum", "certified", "certifiedby",
            "supportedby", "data_error", "proc_error", "error_code"};

    public static String resolveXhtmlTerm(String predicate) {
        for (String link : XHTML_VOCAB_PROPS) {
            if (link.equalsIgnoreCase(predicate)) {
                return XHTML_VOCAB + link;
            }
        }
        return null;
    }

    public static String resolvePowderTerm(String predicate) {
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
            if (!resolveKnownPrefixes || !INITIAL_CONTEXT.containsKey(prefix)) {
                throw new MalformedCURIEException("Unknown prefix");
//                return null;
            } else {
                return INITIAL_CONTEXT.get(prefix) + localName;
            }
        }
        return iriMappings.get(prefix) + localName;
    }

    private CURIE() {
    }
}
