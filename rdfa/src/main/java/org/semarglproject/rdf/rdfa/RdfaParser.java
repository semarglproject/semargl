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
import org.semarglproject.rdf.RdfXmlParser;
import org.semarglproject.rdf.SaxSink;
import org.semarglproject.rdf.TripleSink;
import org.semarglproject.rdf.TripleSource;
import org.semarglproject.ri.CURIE;
import org.semarglproject.ri.IRI;
import org.semarglproject.ri.MalformedCURIEException;
import org.semarglproject.ri.MalformedIRIException;
import org.semarglproject.vocab.RDF;
import org.semarglproject.xml.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class RdfaParser implements SaxSink, TripleSource {

    public static final short RDFA_10 = 0;
    public static final short RDFA_11 = 1;

    private static final String BASE_IF_HEAD_OR_BODY = "bihob";
    private static final String BASE_IF_ROOT_NODE = "birn";
    private static final String PARENT_OBJECT = "poie";
    private static final String BNODE_IF_TYPEOF = RDFa.TYPEOF_ATTR;
    static final String FORWARD = "fwd";
    static final String REVERSE = "rev";

    private static final int PREFIX_LENGTH = FORWARD.length();

    private static final String HTML = "html";
    private static final String BODY = "body";
    private static final String HEAD = "head";
    private static final String BASE = "base";
    private static final String VERSION = "version";

    private static final short FORMAT_UNKNOWN = 0;
    private static final short FORMAT_HTML4 = 1;
    private static final short FORMAT_HTML5 = 2;
    private static final short FORMAT_XML = 3;
    private static final short FORMAT_SVG = 4;

    private static final String SEPARATOR = "\\s+";
    private static final String EMPTY_STRING = "";
    private static final String XHTML_VOCAB = "http://www.w3.org/1999/xhtml/vocab#";
    private static final String XHTML_DEFAULT_XMLNS = "http://www.w3.org/1999/xhtml";
    private static final String METADATA = "metadata";

    // html5 specific
    private static final String DATETIME_ATTR = "datetime";
    private static final String VALUE_ATTR = "value";
    private static final String DATA_ATTR = "data";
    public static final String XML_BASE = "xml:base";
    public static final String RDFA_1_0 = "rdfa 1.0";

    private TripleSink sink;
    private Stack<EvalContext> contextStack = null;
    private String base;

    private String xmlString = null;
    private String xmlStringPred = null;
    private String xmlStringSubj = null;

    private short defaultRdfaVersion = RDFA_11;
    private short rdfaVersion;
    private boolean sinkOutputGraph;
    private boolean sinkProcessorGraph;
    private short documentFormat;
    private int nextBnodeId;

    private boolean rdfXmlInline;
    private RdfXmlParser rdfXmlParser;

    // document bnodes to inner bnodes mapping
    private final Map<String, String> bnodeMapping = new HashMap<String, String>();
    private final Map<String, String> overwriteMappings = new HashMap<String, String>();

    public RdfaParser(boolean sinkOutputGraph, boolean sinkProcessorGraph) {
        setOutput(sinkOutputGraph, sinkProcessorGraph);
    }

    public RdfaParser() {
        this(true, true);
    }

    public void setOutput(boolean sinkOutputGraph, boolean sinkProcessorGraph) {
        this.sinkOutputGraph = sinkOutputGraph;
        this.sinkProcessorGraph = sinkProcessorGraph;
        if (sinkProcessorGraph) {
            defaultRdfaVersion = RDFA_11;
        }
    }

    public void setRdfaVersion(short rdfaVersion) {
        if (rdfaVersion < RDFA_10 || rdfaVersion > RDFA_11) {
            throw new IllegalArgumentException("Unsupported RDFa version");
        }
        defaultRdfaVersion = rdfaVersion;
    }

    @Override
    public void startDocument() {
        contextStack = new Stack<EvalContext>();
        contextStack.push(new EvalContext(base, null, null, null));
        rdfaVersion = defaultRdfaVersion;
        contextStack.peek().iriMappings.put("", XHTML_VOCAB);
        xmlString = null;
        xmlStringPred = null;
        xmlStringSubj = null;
        documentFormat = FORMAT_UNKNOWN;
    }

    private String resolvePredOrDatatype(String value, EvalContext context) throws SAXException {
        try {
            if (rdfaVersion > RDFA_10) {
                return resolveTermOrSafeCURIEOrAbsIri(value, context);
            } else {
                return resolveTermOrSafeCURIE(value, context);
            }
        } catch (MalformedIRIException e) {
            throw new SAXException(new ParseException(e));
        }
    }

    private String resolveTermOrSafeCURIEOrAbsIri(String value, EvalContext context)
            throws MalformedIRIException {
        if (value == null) {
            return null;
        }
        if (IRI.isAbsolute(value)) {
            return value;
        }
        return resolveTermOrSafeCURIE(value, context);
    }

    private String resolveTermOrSafeCURIE(String value, EvalContext context)
            throws MalformedIRIException {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (context.vocab != null && value.matches("[a-zA-Z0-9]+")) {
            return IRI.resolve(base, context.vocab + value);
        }
        if (value.indexOf(':') == -1) {
            String result = CURIE.resolveXhtmlTerm(value);
            if (rdfaVersion == RDFA_10) {
                return result;
            }
            if (result == null) {
                result = CURIE.resolvePowderTerm(value);
                if (result == null) {
                    warning(RDFa.UNRESOLVED_TERM);
                }
            }
            return result;
        }
        try {
            String iri = CURIE.resolve(value, context.iriMappings, rdfaVersion > RDFA_10);
            return IRI.resolve(base, iri);
        } catch (MalformedCURIEException e) {
            warning(RDFa.UNRESOLVED_CURIE);
            return null;
        }
    }

    private String resolveAboutOrResource(String value, EvalContext context) throws ParseException {
        try {
            String result = resolveBNode(value);
            if (result == null) {
                try {
                    String iri = CURIE.resolve(value, context.iriMappings, rdfaVersion > RDFA_10);
                    result = IRI.resolve(base, iri);
                } catch (MalformedCURIEException e) {
                    warning(RDFa.UNRESOLVED_CURIE);
                }
            }
            if (result == null) {
                result = IRI.resolve(base, value);
            }
            return result;
        } catch (MalformedIRIException e) {
            throw new ParseException(e);
        }
    }

    private String resolveBNode(String value) {
        if (value.startsWith(RDF.BNODE_PREFIX)
                || value.startsWith("[" + RDF.BNODE_PREFIX)
                && value.charAt(value.length() - 1) == ']') {
            String name;
            if (value.charAt(0) == '[') {
                name = value.substring(RDF.BNODE_PREFIX.length() + 1, value.length() - 1);
            } else {
                name = value.substring(RDF.BNODE_PREFIX.length());
            }
            if (!bnodeMapping.containsKey(name)) {
                bnodeMapping.put(name, createBnode());
            }
            return bnodeMapping.get(name);
        }
        return null;
    }

    private String coalesce(String tagName, Attributes attrs, EvalContext parent,
                            EvalContext current, String... attrNames) throws ParseException {
        for (String attr : attrNames) {
            if (attrs.getValue(attr) != null) {
                if (attr.equals(RDFa.ABOUT_ATTR) || attr.equals(RDFa.RESOURCE_ATTR)) {
                    String val = attrs.getValue(attr);
                    if (val.equals("[]")) {
                        continue;
                    }
                    return resolveAboutOrResource(val, current);
                } else if (attr.equals(RDFa.HREF_ATTR) || attr.equals(RDFa.SRC_ATTR) || attr.equals(DATA_ATTR)) {
                    try {
                        return IRI.resolve(base, attrs.getValue(attr));
                    } catch (MalformedIRIException e) {
                        throw new ParseException(e);
                    }
                } else if (attr.equals(BNODE_IF_TYPEOF)) {
                    return createBnode();
                }
            } else if (attr.equals(PARENT_OBJECT)) {
                return parent.object;
            } else {
                boolean isHeadOrBody = tagName.equals(HEAD) || tagName.equals(BODY);
                boolean isRoot = contextStack.size() == 1
                        || attrs.getValue(RDFa.TYPEOF_ATTR) != null && isHeadOrBody;
                if (attr.equals(BASE_IF_HEAD_OR_BODY) && isHeadOrBody || attr.equals(BASE_IF_ROOT_NODE) && isRoot) {
                    return base;
                }
            }
        }
        return null;
    }

    private void warning(String warningClass) {
        if (rdfaVersion > RDFA_10 && sinkProcessorGraph) {
            sink.addIriRef(createBnode(), RDF.TYPE, warningClass);
        }
    }

    @Override
    public void startElement(String nsUri, String localName, String qName, Attributes attrs)
            throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.startElement(nsUri, localName, qName, attrs);
            return;
        } else if (documentFormat == FORMAT_SVG && localName.equals(METADATA)) {
            if (rdfXmlParser == null) {
                rdfXmlParser = new RdfXmlParser().streamingTo(sink);
                rdfXmlParser.setBaseUri(base);
                rdfXmlParser.startDocument();
            }
            rdfXmlInline = true;
            return;
        }

        if (contextStack.size() < 4) {
            detectBaseAndFormat(localName, qName, attrs);
        }

        EvalContext parent = contextStack.peek();
        if (parent.parsingLiteral) {
            xmlString += XmlUtils.serializeOpenTag(nsUri, qName, parent.iriMappings, attrs, false);
        }

        if (rdfaVersion > RDFA_10 && attrs.getValue(RDFa.PREFIX_ATTR) != null) {
            String[] pxs = attrs.getValue(RDFa.PREFIX_ATTR).trim().split(SEPARATOR);
            for (int i = 0; i < pxs.length - 1; i += 2) {
                if (!pxs[i].endsWith(":") || pxs[i].length() == 1) {
                    continue;
                }
                startPrefixMapping(pxs[i].substring(0, pxs[i].length() - 1).toLowerCase(),
                        pxs[i + 1]);
            }
        }

        EvalContext current = initElementProfile(qName, attrs, parent);

        boolean skipTerms = rdfaVersion > RDFA_10 && (documentFormat == FORMAT_HTML4 || documentFormat == FORMAT_HTML5)
                && attrs.getValue(RDFa.PROPERTY_ATTR) != null;
        List<String> rels = convertRelRevToList(attrs.getValue(RDFa.REL_ATTR), skipTerms);
        List<String> revs = convertRelRevToList(attrs.getValue(RDFa.REV_ATTR), skipTerms);
        boolean noRelsAndRevs = rels == null && revs == null;

        boolean skipElement = findSubjectAndObject(qName, attrs, noRelsAndRevs, current, parent);

        processRelsAndRevs(attrs, rels, revs, current, parent, skipTerms);

        boolean recurse = processPropertyAttr(qName, attrs, noRelsAndRevs, current, parent);

        pushContext(current, parent, skipElement, recurse);
    }

    private EvalContext initElementProfile(String qName, Attributes attrs, EvalContext parent) {
        EvalContext current = parent.initChildContext(overwriteMappings);
        overwriteMappings.clear();

        if (rdfaVersion > RDFA_10) {
            if (attrs.getValue(RDFa.PROFILE_ATTR) != null) {
                String newProfile = attrs.getValue(RDFa.PROFILE_ATTR) + "#";
                if (current.profile == null) {
                    current.profile = newProfile;
                } else {
                    current.profile = newProfile + ' ' + current.profile;
                }
                if (qName.equalsIgnoreCase(HEAD) && contextStack.size() == 2) {
                    contextStack.get(1).appendToProfile(newProfile);
                }
            }
            if (attrs.getValue(RDFa.VOCAB_ATTR) != null) {
                current.vocab = attrs.getValue(RDFa.VOCAB_ATTR);
                if (current.vocab.length() == 0) {
                    current.vocab = null;
                } else if (sinkOutputGraph) {
                    sink.addIriRef(base, RDFa.USES_VOCABULARY, current.vocab);
                }
            }
        }

        if (attrs.getValue(XmlUtils.XML_LANG) != null) {
            current.lang = attrs.getValue(XmlUtils.XML_LANG);
        } else if (attrs.getValue(XmlUtils.LANG) != null) {
            current.lang = attrs.getValue(XmlUtils.LANG);
        }
        if (current.lang != null && current.lang.length() == 0) {
            current.lang = null;
        }
        return current;
    }

    private void pushContext(EvalContext current, EvalContext parent, boolean skipElement, boolean recurse) {
        current.parsingLiteral = !recurse;
        if (!skipElement && current.subject != null) {
            List<Object> incompl = parent.incomplTriples;
            String subj = parent.subject;
            for (Object obj : incompl) {
                if (obj instanceof String) {
                    if (sinkOutputGraph) {
                        String pred = (String) obj;
                        if (pred.startsWith(FORWARD)) {
                            sink.addNonLiteral(subj, pred.substring(PREFIX_LENGTH), current.subject);
                        } else {
                            sink.addNonLiteral(current.subject, pred.substring(PREFIX_LENGTH), subj);
                        }
                    }
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) obj;
                    list.add(current.subject);
                }
            }
        }
        if (current.parsingLiteral) {
            xmlString = "";
            xmlStringPred = current.properties;
            xmlStringSubj = current.subject == null ? parent.subject : current.subject;
        }
        if (current.parsingLiteral || skipElement) {
            current.subject = parent.subject;
            current.object = parent.object;
            current.incomplTriples = parent.incomplTriples;
            current.objectLit = null;
            current.objectLitDt = parent.objectLitDt;
            if (current.objectLitDt != null) {
                current.objectLit = "";
            }
            current.properties = null;
            contextStack.push(current);
        } else {
            saveCurrentContext(current, parent);
        }
    }

    private void detectBaseAndFormat(String localName, String qName, Attributes attrs) {
        if (documentFormat == FORMAT_UNKNOWN) {
            if (localName.equals("svg")) {
                documentFormat = FORMAT_SVG;
            } else if (localName.equalsIgnoreCase(HTML)) {
                documentFormat = FORMAT_HTML4;
            } else {
                documentFormat = FORMAT_XML;
            }
        }

        boolean xmlBaseF = (documentFormat == FORMAT_XML || documentFormat == FORMAT_SVG)
                && attrs.getValue(XML_BASE) != null;
        if (xmlBaseF || qName.equalsIgnoreCase(BASE) && attrs.getValue(RDFa.HREF_ATTR) != null) {
            String oldBase = base;
            base = xmlBaseF ? attrs.getValue(XML_BASE) : attrs.getValue(RDFa.HREF_ATTR);
            base = base.replaceAll("#.*", "");
            for (EvalContext ctx : contextStack) {
                ctx.updateBase(oldBase, base);
            }
        }
        if (qName.equalsIgnoreCase(HTML) && attrs.getValue(VERSION) != null
                && attrs.getValue(VERSION).toLowerCase().contains(RDFA_1_0)) {
            rdfaVersion = RDFA_10;
        }
    }

    private void processRelsAndRevs(Attributes attrs, List<String> rels, List<String> revs, EvalContext current, EvalContext parent, boolean skipTerms) throws SAXException {
        // don't fill parent list if subject was changed at this
        // or previous step by current.parentObject
        if (rdfaVersion > RDFA_10 && current.subject != null
                && (current.subject != parent.object || parent.object != parent.subject)) {
            current.listMapping = new HashMap<String, List<Object>>();
        }

        if (rels != null) {
            if (rdfaVersion > RDFA_10 && attrs.getValue(RDFa.INLIST_ATTR) != null) {
                for (String predicate : rels) {
                    if (skipTerms && predicate.indexOf(':') == -1) {
                        continue;
                    }
                    String iri = resolvePredOrDatatype(predicate, current);
                    if (iri == null) {
                        continue;
                    }
                    if (!current.listMapping.containsKey(iri)) {
                        current.listMapping.put(iri, new ArrayList<Object>());
                    }
                    List<Object> list = current.listMapping.get(iri);
                    if (current.object != null) {
                        list.add(current.object);
                    } else {
                        current.incomplTriples.add(list);
                    }
                }
            } else {
                for (String predicate : rels) {
                    if (skipTerms && predicate.indexOf(':') == -1) {
                        continue;
                    }
                    String iri = resolvePredOrDatatype(predicate, current);
                    if (iri == null) {
                        continue;
                    }
                    if (current.object != null) {
                        if (sinkOutputGraph) {
                            sink.addNonLiteral(current.subject, iri, current.object);
                        }
                    } else {
                        current.incomplTriples.add(FORWARD + iri);
                    }
                }
            }
        }
        if (revs != null) {
            for (String predicate : revs) {
                String iri = resolvePredOrDatatype(predicate, current);
                if (iri == null) {
                    continue;
                }
                if (current.object != null) {
                    if (sinkOutputGraph) {
                        sink.addNonLiteral(current.object, iri, current.subject);
                    }
                } else {
                    current.incomplTriples.add(REVERSE + iri);
                }
            }
        }
        if (current.object == null && (rels != null || revs != null)) {
            current.object = createBnode();
        }

    }

    private boolean processPropertyAttr(String qName, Attributes attrs, boolean noRelsAndRevs, EvalContext current, EvalContext parent) throws SAXException {
        boolean recurse = true;
        String propValueNonLit = null;
        LiteralNode propValueLit = null;
        if (attrs.getValue(RDFa.PROPERTY_ATTR) != null) {
            String datatype = attrs.getValue(RDFa.DATATYPE_ATTR);
            String content = attrs.getValue(RDFa.CONTENT_ATTR);

            if (documentFormat == FORMAT_HTML5) {
                if (attrs.getValue(VALUE_ATTR) != null) {
                    content = attrs.getValue(VALUE_ATTR);
                }
                if (attrs.getValue(DATETIME_ATTR) != null) {
                    if (datatype == null) {
                        datatype = XSD.DATE_TIME;
                    }
                    content = attrs.getValue(DATETIME_ATTR);
                } else if (qName.equals("time") && datatype == null) {
                    datatype = XSD.DATE_TIME;
                }
            }

            String dt = resolvePredOrDatatype(datatype, current);
            if (dt == null && datatype != null && datatype.length() > 0) {
                datatype = null; // ignore incorrect datatype
            }
            if (datatype != null && datatype.length() > 0 && !RDF.XML_LITERAL.equals(dt)) {
                if (content != null) {
                    try {
                        propValueLit = parseTypedLiteral(content, dt);
                    } catch (ParseException e) {
                        propValueLit = new PlainLiteral(content, current.lang);
                    }
                } else {
                    current.objectLitDt = dt;
                }
            } else if (rdfaVersion > RDFA_10) {
                if (datatype != null) {
                    if (datatype.length() == 0) {
                        if (content != null) {
                            propValueLit = new PlainLiteral(content, current.lang);
                        } else {
                            current.objectLitDt = EMPTY_STRING;
                        }
                    } else if (datatype.length() > 0) { // == rdf:XMLLiteral
                        current.objectLitDt = RDF.XML_LITERAL;
                        recurse = false;
                    }
                } else {
                    if (content != null) {
                        propValueLit = new PlainLiteral(content, current.lang);
                    } else if (attrs.getValue(RDFa.CONTENT_ATTR) == null
                            && attrs.getValue(VALUE_ATTR) == null
                            && noRelsAndRevs) {
                        try {
                            propValueNonLit = coalesce(qName, attrs, parent, current,
                                    RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                        } catch (ParseException e) {
                            warning(RDFa.WARNING);
                            saveCurrentContext(current, parent);
                        }
                    }
                    if (propValueLit == null && propValueNonLit == null
                            && attrs.getValue(RDFa.ABOUT_ATTR) == null
                            && attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                        propValueNonLit = current.object;
                    }
                    if (propValueLit == null && propValueNonLit == null) {
                        current.objectLitDt = EMPTY_STRING;
                    }
                }
            } else {
                if (content != null) {
                    propValueLit = new PlainLiteral(content, current.lang);
                } else {
                    if (datatype == null) {
                        current.objectLitDt = RDF.XML_LITERAL;// RDFS_LITERAL;
                        recurse = false;
                    } else if (datatype.length() > 0) { // == rdf:XMLLiteral
                        current.objectLitDt = RDF.XML_LITERAL;
                        recurse = false;
                    } else {
                        current.objectLitDt = EMPTY_STRING;
                    }
                }
            }

            String[] parts = attrs.getValue(RDFa.PROPERTY_ATTR).trim().split(SEPARATOR);
            for (String pred : parts) {
                String iri = resolvePredOrDatatype(pred, current);
                if (iri == null) {
                    continue;
                }
                if (propValueLit != null || propValueNonLit != null) {
                    if (rdfaVersion > RDFA_10 && attrs.getValue(RDFa.INLIST_ATTR) != null) {
                        if (!current.listMapping.containsKey(iri)) {
                            current.listMapping.put(iri, new ArrayList<Object>());
                        }
                        List<Object> list = current.listMapping.get(iri);
                        if (propValueLit != null) {
                            list.add(propValueLit);
                        } else {
                            list.add(propValueNonLit);
                        }
                    } else {
                        if (propValueLit != null) {
                            addLiteralTriple(sink, current.subject, iri, propValueLit);
                        } else if (sinkOutputGraph) {
                            sink.addNonLiteral(current.subject, iri, propValueNonLit);
                        }
                    }
                } else if (current.properties == null) {
                    if (rdfaVersion > RDFA_10 && attrs.getValue(RDFa.INLIST_ATTR) != null) {
                        current.properties = RDFa.INLIST_ATTR + " " + iri;
                    } else {
                        current.properties = iri;
                    }
                } else {
                    current.properties += " " + iri;
                }
            }
            if (current.properties == null) {
                current.objectLitDt = null;
                recurse = true;
            }
        }
        return recurse;
    }

    private boolean findSubjectAndObject(String qName, Attributes attrs, boolean noRelsAndRevs, EvalContext current, EvalContext parent) throws SAXException {
        boolean skipElement = false;
        String typedRes = null;
        try {
            if (rdfaVersion > RDFA_10) {
                if (noRelsAndRevs) {
                    if (attrs.getValue(RDFa.PROPERTY_ATTR) != null
                            && attrs.getValue(RDFa.CONTENT_ATTR) == null
                            && attrs.getValue(VALUE_ATTR) == null
                            && attrs.getValue(RDFa.DATATYPE_ATTR) == null) {
                        current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                                BASE_IF_ROOT_NODE, PARENT_OBJECT);

                        if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                            current.object = coalesce(qName, attrs, parent, current,
                                    RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                            if (current.object == null) {
                                if (current.subject != null) {
                                    current.object = current.subject;
                                } else {
                                    current.object = createBnode();
                                }
                            }
                            typedRes = current.object;
                        }
                    } else {
                        current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                                RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR, BASE_IF_ROOT_NODE,
                                BNODE_IF_TYPEOF, PARENT_OBJECT);
                        if (current.subject == parent.object && attrs.getValue(RDFa.PROPERTY_ATTR) == null) {
                            skipElement = true;
                        }
                        if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                            typedRes = current.subject;
                        }
                    }
                } else {
                    current.object = coalesce(qName, attrs, parent, current, RDFa.RESOURCE_ATTR, DATA_ATTR,
                            RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                            BASE_IF_ROOT_NODE, PARENT_OBJECT, PARENT_OBJECT);
                    if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                        if (attrs.getValue(RDFa.ABOUT_ATTR) != null) {
                            typedRes = current.subject;
                        } else {
                            if (current.object == null) {
                                current.object = createBnode();
                            }
                            typedRes = current.object;
                        }
                    }
                }
            } else {
                if (noRelsAndRevs) {
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                            RDFa.SRC_ATTR, RDFa.RESOURCE_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR,
                            BASE_IF_HEAD_OR_BODY, BNODE_IF_TYPEOF, PARENT_OBJECT);
                    if (current.subject == parent.object && attrs.getValue(RDFa.PROPERTY_ATTR) == null) {
                        skipElement = true;
                    }
                } else {
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                            RDFa.SRC_ATTR, BASE_IF_HEAD_OR_BODY, BNODE_IF_TYPEOF, PARENT_OBJECT);
                    current.object = coalesce(qName, attrs, parent, current, RDFa.RESOURCE_ATTR,
                            RDFa.HREF_ATTR);
                }
                if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                    typedRes = current.subject;
                }
            }
        } catch (ParseException e) {
            warning(RDFa.WARNING);
            saveCurrentContext(current, parent);
        }
        if (typedRes != null) {
            for (String type : attrs.getValue(RDFa.TYPEOF_ATTR).split(SEPARATOR)) {
                String iri = resolvePredOrDatatype(type, current);
                if (iri == null) {
                    continue;
                }
                if (sinkOutputGraph) {
                    sink.addIriRef(typedRes, RDF.TYPE, iri);
                }
            }
        }
        return skipElement;
    }

    private static List<String> convertRelRevToList(String propertyVal, boolean skipTerms) {
        List<String> result = null;
        if (propertyVal != null) {
            result = new ArrayList<String>(Arrays.asList(propertyVal.split(SEPARATOR)));
            if (skipTerms) {
                for (Iterator<String> li = result.iterator(); li.hasNext(); ) {
                    String rel = li.next();
                    if (rel.indexOf(':') == -1) {
                        li.remove();
                    }
                }
                if (result.isEmpty()) {
                    result = null;
                }
            }
        }
        return result;
    }

    private void saveCurrentContext(EvalContext current, EvalContext parent) {
        if (current.subject == null) {
            current.subject = parent.subject;
        }
        if (current.object == null) {
            current.object = current.subject;
        }
        if (current.objectLitDt != null || parent.objectLitDt != null) {
            current.objectLit = "";
        }
        contextStack.push(current);
    }

    private TypedLiteral parseTypedLiteral(String content, String dt) throws ParseException {
        if (dt.equals(XSD.DATE_TIME)) {
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

    private static void addLiteralTriple(TripleSink th, String subject, String pred,
                                         LiteralNode literal) {
        if (literal instanceof TypedLiteral) {
            TypedLiteral tl = (TypedLiteral) literal;
            th.addTypedLiteral(subject, pred, tl.getContent(), tl.getType());
        } else {
            PlainLiteral pl = (PlainLiteral) literal;
            th.addPlainLiteral(subject, pred, pl.getContent(), pl.getLang());
        }
    }

    @Override
    public void endElement(String nsUri, String localName, String qName) throws SAXException {
        if (rdfXmlInline) {
            if (documentFormat == FORMAT_SVG && localName.equals(METADATA)) {
                rdfXmlParser.endDocument();
                rdfXmlParser = null;
                rdfXmlInline = false;
            } else {
                rdfXmlParser.endElement(nsUri, localName, qName);
            }
            return;
        }
        EvalContext current = contextStack.pop();

        if (current.parsingLiteral && xmlString != null) {
            if (sinkOutputGraph) {
                if (rdfaVersion == RDFA_10 && !xmlString.contains("<")) {
                    for (String pred : xmlStringPred.split(SEPARATOR)) {
                        sink.addPlainLiteral(xmlStringSubj, pred, xmlString, current.lang);  // TODO: RDFa 1.0 case 212
                    }
                } else {
                    for (String pred : xmlStringPred.split(SEPARATOR)) {
                        sink.addTypedLiteral(xmlStringSubj, pred, xmlString, RDF.XML_LITERAL);
                    }
                }
            }
            xmlString = null;
        }
        if (xmlString != null) {
            xmlString += "</" + qName + ">";
        }

        if (contextStack.isEmpty()) {
            return;
        }

        EvalContext parent = contextStack.peek();
        String content = current.objectLit;
        if (content != null) {
            if (!parent.parsingLiteral && parent.objectLit != null) {
                parent.setObjectLit(parent.objectLit + content);
            } else {
                String dt = current.objectLitDt;
                boolean inlist = current.properties.startsWith(RDFa.INLIST_ATTR + " ");

                if (inlist) {
                    LiteralNode currObjectLit;
                    if (dt.isEmpty()) {// plain
                        currObjectLit = new PlainLiteral(content, current.lang);
                    } else {
                        try {
                            currObjectLit = parseTypedLiteral(content, dt);
                        } catch (ParseException e) {
                            currObjectLit = new PlainLiteral(content, current.lang);
                        }
                    }
                    for (String predIri : current.properties.substring(
                            RDFa.INLIST_ATTR.length() + 1).split(SEPARATOR)) {
                        if (!current.listMapping.containsKey(predIri)) {
                            current.listMapping.put(predIri, new ArrayList<Object>());
                        }
                        current.listMapping.get(predIri).add(currObjectLit);
                    }
                } else if (sinkOutputGraph) {
                    if (dt.isEmpty()) {// plain
                        for (String predIri : current.properties.split(SEPARATOR)) {
                            sink.addPlainLiteral(current.subject, predIri, content, current.lang);
                        }
                    } else {
                        for (String predIri : current.properties.split(SEPARATOR)) {
                            try {
                                TypedLiteral lit = parseTypedLiteral(content, dt);
                                sink.addTypedLiteral(current.subject, predIri, lit.getContent(), lit.getType());
                            } catch (ParseException e) {
                                sink.addPlainLiteral(current.subject, predIri, content, current.lang);
                            }
                        }
                    }
                }
            }
        }

        if (parent.listMapping == current.listMapping) {
            return;
        }

        Map<String, List<Object>> list = current.listMapping;
        if (sinkOutputGraph) {
            for (String pred : list.keySet()) {
                String prev = null;
                String start = null;
                for (Object res : list.get(pred)) {
                    String child = createBnode();
                    if (res instanceof LiteralNode) {
                        addLiteralTriple(sink, child, RDF.FIRST, (LiteralNode) res);
                    } else {
                        sink.addNonLiteral(child, RDF.FIRST, (String) res);
                    }
                    if (prev == null) {
                        start = child;
                    } else {
                        sink.addNonLiteral(prev, RDF.REST, child);
                    }
                    prev = child;
                }
                if (start == null) {
                    sink.addIriRef(current.subject, pred, RDF.NIL);
                } else {
                    sink.addIriRef(prev, RDF.REST, RDF.NIL);
                    sink.addNonLiteral(current.subject, pred, start);
                }
                list.remove(pred);
            }
        } else {
            list.clear();
        }
    }

    private String createBnode() {
        return RDF.BNODE_PREFIX + 'n' + nextBnodeId++;
    }

    @Override
    public void characters(char[] buffer, int start, int length) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.characters(buffer, start, length);
            return;
        }
        EvalContext parent = contextStack.peek();
        if (xmlString != null) {
            xmlString += String.copyValueOf(buffer, start, length);
        }
        if (parent.objectLit != null) {
            parent.addContent(String.copyValueOf(buffer, start, length));
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.startPrefixMapping(prefix, uri);
            return;
        }
        // TODO: check for valid prefix
        if (prefix.length() == 0 && XHTML_DEFAULT_XMLNS.equalsIgnoreCase(uri)) {
            overwriteMappings.put(prefix, XHTML_VOCAB);
        } else {
            if (CURIE.INITIAL_CONTEXT.containsKey(prefix) && !CURIE.INITIAL_CONTEXT.get(prefix).equals(uri)) {
                warning(RDFa.PREFIX_REDEFINITION);
            }
            overwriteMappings.put(prefix, uri);
        }
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.endPrefixMapping(prefix);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }

    @Override
    public void startDTD(String s, String s1, String s2) throws SAXException {
        if (s1 == null) {
            if (HTML.equalsIgnoreCase(s)) {
                documentFormat = FORMAT_HTML5;
            }
        } else {
            s1 = s1.toLowerCase();
            if (s1.contains(HTML)) {
                documentFormat = FORMAT_HTML4;
            }
            if (s1.contains(RDFA_1_0)) {
                rdfaVersion = RDFA_10;
            }
        }
    }

    @Override
    public void endDTD() throws SAXException {
    }

    @Override
    public void startEntity(String s) throws SAXException {
    }

    @Override
    public void endEntity(String s) throws SAXException {
    }

    @Override
    public void startCDATA() throws SAXException {
    }

    @Override
    public void endCDATA() throws SAXException {
    }

    @Override
    public void comment(char[] chars, int i, int i1) throws SAXException {
    }

    @Override
    public void startStream() {
        sink.startStream();
    }

    @Override
    public void endStream() {
        sink.endStream();
    }

    @Override
    public void setBaseUri(String baseUri) {
        base = baseUri;
    }

    @Override
    public RdfaParser streamingTo(TripleSink sink) {
        this.sink = sink;
        return this;
    }

    @Override
    public ParseException processException(SAXException e) {
        if (rdfaVersion > RDFA_10 && sinkProcessorGraph) {
            sink.addIriRef(createBnode(), RDF.TYPE, RDFa.ERROR);
        }
        Throwable cause = e.getCause();
        if (cause instanceof ParseException) {
            return (ParseException) cause;
        }
        return new ParseException(e);
    }

    private static final class EvalContext {
        private String subject;
        private String object;
        private final Map<String, String> iriMappings;
        private List<Object> incomplTriples;
        private String lang;
        private String objectLit;
        private String objectLitDt;
        private String properties;
        private String vocab;
        private String profile;
        private boolean parsingLiteral;
        private Map<String, List<Object>> listMapping;

        EvalContext(String subject, String lang, String vocab, String profile) {
            this.subject = subject;
            this.object = null;
            this.iriMappings = new HashMap<String, String>();
            this.incomplTriples = new ArrayList<Object>();
            this.lang = lang;
            this.objectLit = null;
            this.objectLitDt = null;
            this.vocab = vocab;
            this.profile = profile;
            this.properties = null;
            this.parsingLiteral = false;
            this.listMapping = new HashMap<String, List<Object>>();
        }

        EvalContext initChildContext(Map<String, String> overwriteMappings) {
            EvalContext result = new EvalContext(null, lang, vocab, profile);
            result.listMapping = listMapping;
            result.iriMappings.putAll(iriMappings);
            result.iriMappings.putAll(overwriteMappings);
            return result;
        }

        void addContent(String content) {
            objectLit += content;
        }

        void setObjectLit(String objectLit) {
            this.objectLit = objectLit;
        }

        void updateBase(String oldBase, String base) {
            if (object != null && object.equals(oldBase)) {
                object = base;
            }
            if (subject != null && subject.equals(oldBase)) {
                subject = base;
            }
        }

        void appendToProfile(String value) {
            if (profile == null) {
                profile = value;
            } else {
                profile += " " + value;
            }
        }
    }
}
