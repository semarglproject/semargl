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
import org.semarglproject.ri.MalformedIRIException;
import org.semarglproject.vocab.RDF;
import org.semarglproject.vocab.RDFa;
import org.semarglproject.xml.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public final class RdfaParser implements SaxSink, TripleSource, ProcessorGraphHandler, ResourceResolver, TripleSink {

    // prefixes for incompleted triples should be of same length
    private static final String FORWARD = "fwd";
    private static final String REVERSE = "rev";
    private static final int PREFIX_LENGTH = FORWARD.length();

    private static final String BODY = "body";
    private static final String HEAD = "head";
    private static final String VERSION = "version";
    private static final String METADATA = "metadata";

    private static final String SEPARATOR = "\\s+";
    private static final String PLAIN_LITERAL = "";
    private static final String XHTML_DEFAULT_XMLNS = "http://www.w3.org/1999/xhtml";

    private static final String XHTML_VOCAB = "http://www.w3.org/1999/xhtml/vocab#";

    // html5 support
    private static final String DATETIME_ATTR = "datetime";
    private static final String VALUE_ATTR = "value";
    private static final String DATA_ATTR = "data";
    private static final String XML_BASE = "xml:base";
    static final String AUTODETECT_DATE_DATATYPE = "AUTODETECT_DATE_DATATYPE";

    // keys for coalesce method
    private static final String BASE_IF_HEAD_OR_BODY = "bihob";
    private static final String BASE_IF_ROOT_NODE = "birn";
    private static final String PARENT_OBJECT = "poie";
    private static final String BNODE_IF_TYPEOF = RDFa.TYPEOF_ATTR;

    private TripleSink sink;
    private Stack<EvalContext> contextStack = null;

    private String xmlString = null;
    private String xmlStringPred = null;
    private String xmlStringSubj = null;

    private short defaultRdfaVersion = RDFa.VERSION_10;
    private boolean sinkOutputGraph;
    private boolean sinkProcessorGraph;

    private DocumentContext dh;

    private boolean rdfXmlInline;
    private RdfXmlParser rdfXmlParser;

    // document bnodes to inner bnodes mapping
    private final Map<String, String> overwriteMappings = new HashMap<String, String>();

    public RdfaParser(boolean sinkOutputGraph, boolean sinkProcessorGraph) {
        setOutput(sinkOutputGraph, sinkProcessorGraph);
        dh = new DocumentContext(null, defaultRdfaVersion);
        contextStack = new Stack<EvalContext>();
    }

    public RdfaParser() {
        this(true, true);
    }

    public void setOutput(boolean sinkOutputGraph, boolean sinkProcessorGraph) {
        this.sinkOutputGraph = sinkOutputGraph;
        this.sinkProcessorGraph = sinkProcessorGraph;
        if (sinkProcessorGraph) {
            defaultRdfaVersion = RDFa.VERSION_11;
        }
    }

    public void setRdfaVersion(short rdfaVersion) {
        if (rdfaVersion < RDFa.VERSION_10 || rdfaVersion > RDFa.VERSION_11) {
            throw new IllegalArgumentException("Unsupported RDFa version");
        }
        defaultRdfaVersion = rdfaVersion;
    }

    @Override
    public void startDocument() {
        EvalContext initialContext = new EvalContext(this, dh.base);
        initialContext.iriMappings.put("", XHTML_VOCAB);
        contextStack.push(initialContext);

        dh.clear(defaultRdfaVersion);
        xmlString = null;
        xmlStringPred = null;
        xmlStringSubj = null;
    }

    @Override
    public void endDocument() throws SAXException {
        contextStack.clear();
    }

    @Override
    public void startElement(String nsUri, String localName, String qName, Attributes attrs)
            throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.startElement(nsUri, localName, qName, attrs);
            return;
        } else if (dh.documentFormat == DocumentContext.FORMAT_SVG && localName.equals(METADATA)) {
            if (rdfXmlParser == null) {
                rdfXmlParser = new RdfXmlParser().streamingTo(this);
                rdfXmlParser.setBaseUri(dh.base);
                rdfXmlParser.startDocument();
            }
            rdfXmlInline = true;
            return;
        }

        if (contextStack.size() < 4) {
            String oldBase = dh.base;
            dh.detectBaseAndFormat(localName, qName, attrs.getValue(XML_BASE),
                    attrs.getValue(RDFa.HREF_ATTR), attrs.getValue(VERSION));
            if (!dh.base.equals(oldBase)) {
                for (EvalContext ctx : contextStack) {
                    ctx.updateBase(oldBase, dh.base);
                }
            }
        }

        EvalContext parent = contextStack.peek();
        if (parent.parsingLiteral) {
            xmlString += XmlUtils.serializeOpenTag(nsUri, qName, parent.iriMappings, attrs, false);
        }

        if (dh.rdfaVersion > RDFa.VERSION_10 && attrs.getValue(RDFa.PREFIX_ATTR) != null) {
            String[] pxs = attrs.getValue(RDFa.PREFIX_ATTR).trim().split(SEPARATOR);
            for (int i = 0; i < pxs.length - 1; i += 2) {
                if (!pxs[i].endsWith(":") || pxs[i].length() == 1) {
                    continue;
                }
                startPrefixMapping(pxs[i].substring(0, pxs[i].length() - 1).toLowerCase(), pxs[i + 1]);
            }
        }

        String lang = attrs.getValue(XmlUtils.XML_LANG);
        if (lang == null) {
            lang = attrs.getValue(XmlUtils.LANG);
        }
        EvalContext current = parent.initChildContext(attrs.getValue(RDFa.PROFILE_ATTR),
                attrs.getValue(RDFa.VOCAB_ATTR), lang, overwriteMappings, dh.rdfaVersion);
        overwriteMappings.clear();

        boolean skipTerms = dh.rdfaVersion > RDFa.VERSION_10 && dh.isHtmlDocument()
                && attrs.getValue(RDFa.PROPERTY_ATTR) != null;
        List<String> rels = convertRelRevToList(attrs.getValue(RDFa.REL_ATTR), skipTerms);
        List<String> revs = convertRelRevToList(attrs.getValue(RDFa.REV_ATTR), skipTerms);
        boolean noRelsAndRevs = rels == null && revs == null;

        boolean skipElement = findSubjectAndObject(qName, attrs, noRelsAndRevs, current, parent);

        processRelsAndRevs(attrs, rels, revs, current, parent, skipTerms);

        boolean recurse = processPropertyAttr(qName, attrs, noRelsAndRevs, current, parent);

        pushContext(current, parent, skipElement, recurse);
    }

    private static List<String> convertRelRevToList(String propertyVal, boolean skipTerms) {
        if (propertyVal == null) {
            return null;
        }
        List<String> result = new ArrayList<String>(Arrays.asList(propertyVal.split(SEPARATOR)));
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
        return result;
    }

    private boolean findSubjectAndObject(String qName, Attributes attrs, boolean noRelAndRev, EvalContext current, EvalContext parent) throws SAXException {
        boolean skipElement = false;
        String newSubject = null;
        try {
            if (dh.rdfaVersion > RDFa.VERSION_10) {
                if (noRelAndRev) {
                    // RDFa Core 1.1 processing sequence step 5
                    if (attrs.getValue(RDFa.PROPERTY_ATTR) != null && attrs.getValue(RDFa.CONTENT_ATTR) == null
                            && attrs.getValue(VALUE_ATTR) == null && attrs.getValue(RDFa.DATATYPE_ATTR) == null) {
                        // RDFa Core 1.1 processing sequence step 5.1
                        current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                                BASE_IF_ROOT_NODE, PARENT_OBJECT);

                        if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                            current.object = coalesce(qName, attrs, parent, current,
                                    RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                            if (current.object == null) {
                                if (current.subject != null) {
                                    current.object = current.subject;
                                } else {
                                    current.object = dh.createBnode();
                                }
                            }
                            newSubject = current.object;
                        }
                    } else {
                        // RDFa Core 1.1 processing sequence step 5.2
                        current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                                RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR, BASE_IF_ROOT_NODE,
                                BNODE_IF_TYPEOF, PARENT_OBJECT);
                        if (current.subject == parent.object && attrs.getValue(RDFa.PROPERTY_ATTR) == null) {
                            skipElement = true;
                        }
                        if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                            newSubject = current.subject;
                        }
                    }
                } else {
                    // RDFa Core 1.1 processing sequence step 6
                    current.object = coalesce(qName, attrs, parent, current, RDFa.RESOURCE_ATTR, DATA_ATTR,
                            RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                            BASE_IF_ROOT_NODE, PARENT_OBJECT, PARENT_OBJECT);
                    if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                        if (attrs.getValue(RDFa.ABOUT_ATTR) != null) {
                            newSubject = current.subject;
                        } else {
                            if (current.object == null) {
                                current.object = dh.createBnode();
                            }
                            newSubject = current.object;
                        }
                    }
                }
            } else {
                if (noRelAndRev) {
                    // RDFa Core 1.0 processing sequence step 4
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR, RDFa.SRC_ATTR,
                            RDFa.RESOURCE_ATTR, RDFa.HREF_ATTR, BASE_IF_HEAD_OR_BODY, BNODE_IF_TYPEOF, PARENT_OBJECT);
                    if (current.subject == parent.object && attrs.getValue(RDFa.PROPERTY_ATTR) == null) {
                        skipElement = true;
                    }
                } else {
                    // RDFa Core 1.0 processing sequence step 5
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR, RDFa.SRC_ATTR,
                            BASE_IF_HEAD_OR_BODY, BNODE_IF_TYPEOF, PARENT_OBJECT);
                    current.object = coalesce(qName, attrs, parent, current, RDFa.RESOURCE_ATTR, RDFa.HREF_ATTR);
                }
                if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                    newSubject = current.subject;
                }
            }
        } catch (ParseException e) {
            warning(RDFa.WARNING);
            pushCurrentContext(current, parent);
        }
        if (newSubject != null) {
            // RDFa Core 1.0 processing sequence step 6
            // RDFa Core 1.1 processing sequence step 7
            for (String type : attrs.getValue(RDFa.TYPEOF_ATTR).split(SEPARATOR)) {
                String iri = current.resolvePredOrDatatype(type, dh.rdfaVersion);
                if (iri == null) {
                    continue;
                }
                addIriRef(newSubject, RDF.TYPE, iri);
            }
        }
        return skipElement;
    }

    /**
     * Iterates through attribute names list and returns first not null
     * value of attribute with such name. Also processes special cases
     * if no such attributes found:
     * <ul>
     *     <li>BNODE_IF_TYPEOF - returns new bnode if typeof attr found</li>
     *     <li>PARENT_OBJECT - returns parent.object</li>
     *     <li>BASE_IF_HEAD_OR_BODY - returns base if processing head or body node in HTML</li>
     * </ul>
     *
     * @param tagName name of processed element
     * @param attrs attribute list
     * @param parent parent context
     * @param current current context
     * @param attrNames prioritized list of attributes
     * @throws ParseException
     */
    private String coalesce(String tagName, Attributes attrs, EvalContext parent,
                            EvalContext current, String... attrNames) throws ParseException {
        for (String attr : attrNames) {
            if (attrs.getValue(attr) != null) {
                if (attr.equals(RDFa.ABOUT_ATTR) || attr.equals(RDFa.RESOURCE_ATTR)) {
                    String val = attrs.getValue(attr);
                    if (val.equals("[]")) {
                        continue;
                    }
                    return current.resolveAboutOrResource(val, dh.rdfaVersion);
                } else if (attr.equals(RDFa.HREF_ATTR) || attr.equals(RDFa.SRC_ATTR) || attr.equals(DATA_ATTR)) {
                    try {
                        return resolveIri(attrs.getValue(attr));
                    } catch (MalformedIRIException e) {
                        throw new ParseException(e);
                    }
                } else if (attr.equals(BNODE_IF_TYPEOF)) {
                    return dh.createBnode();
                }
            } else if (attr.equals(PARENT_OBJECT)) {
                return parent.object;
            } else {
                boolean isHeadOrBody = tagName.equals(HEAD) || tagName.equals(BODY);
                boolean isRoot = contextStack.size() == 1 || attrs.getValue(RDFa.TYPEOF_ATTR) != null && isHeadOrBody;
                if (isHeadOrBody && attr.equals(BASE_IF_HEAD_OR_BODY) || isRoot && attr.equals(BASE_IF_ROOT_NODE)) {
                    return dh.base;
                }
            }
        }
        return null;
    }

    private void processRelsAndRevs(Attributes attrs, List<String> rels, List<String> revs, EvalContext current, EvalContext parent, boolean skipTerms) throws SAXException {
        // don't fill parent list if subject was changed at this
        // or previous step by current.parentObject
        if (dh.rdfaVersion > RDFa.VERSION_10 && current.subject != null
                && (current.subject != parent.object || parent.object != parent.subject)) {
            // RDFa Core 1.1 processing sequence step 8
            current.listMapping = new HashMap<String, List<Object>>();
        }

        if (rels != null) {
            if (dh.rdfaVersion > RDFa.VERSION_10 && attrs.getValue(RDFa.INLIST_ATTR) != null) {
                // RDFa Core 1.1 processing sequence steps 9 and 10
                for (String predicate : rels) {
                    if (skipTerms && predicate.indexOf(':') == -1) {
                        continue;
                    }
                    String iri = current.resolvePredOrDatatype(predicate, dh.rdfaVersion);
                    if (iri == null) {
                        continue;
                    }
                    List<Object> list = current.getMappingForIri(iri);
                    if (current.object != null) {
                        list.add(current.object);
                    } else {
                        current.incomplTriples.add(list);
                    }
                }
            } else { // RDFa Core 1.0 processing sequence steps 7 and 8
                for (String predicate : rels) {
                    if (skipTerms && predicate.indexOf(':') == -1) {
                        continue;
                    }
                    String iri = current.resolvePredOrDatatype(predicate, dh.rdfaVersion);
                    if (iri == null) {
                        continue;
                    }
                    if (current.object != null) {
                        addNonLiteral(current.subject, iri, current.object);
                    } else {
                        current.incomplTriples.add(FORWARD + iri);
                    }
                }
            }
        }
        if (revs != null) {
            for (String predicate : revs) {
                // RDFa Core 1.1 processing sequence steps 9 and 10
                String iri = current.resolvePredOrDatatype(predicate, dh.rdfaVersion);
                if (iri == null) {
                    continue;
                }
                if (current.object != null) {
                    addNonLiteral(current.object, iri, current.subject);
                } else {
                    current.incomplTriples.add(REVERSE + iri);
                }
            }
        }
        if (current.object == null && (rels != null || revs != null)) {
            current.object = dh.createBnode();
        }
    }

    private boolean processPropertyAttr(String qName, Attributes attrs, boolean noRelsAndRevs,
                                        EvalContext current, EvalContext parent) throws SAXException {
        boolean recurse = true;
        if (attrs.getValue(RDFa.PROPERTY_ATTR) == null) {
            return recurse;
        }

        String objectNonLit = null;   // object literal value in case it isn't literal
        LiteralNode objectLit = null; // object literal value in case it is literal

        String datatype = attrs.getValue(RDFa.DATATYPE_ATTR);
        String content = attrs.getValue(RDFa.CONTENT_ATTR);

        if (dh.documentFormat == DocumentContext.FORMAT_HTML5) {
            if (attrs.getValue(VALUE_ATTR) != null) {
                content = attrs.getValue(VALUE_ATTR);
            }
            if (attrs.getValue(DATETIME_ATTR) != null) {
                if (datatype == null) {
                    datatype = AUTODETECT_DATE_DATATYPE;
                }
                content = attrs.getValue(DATETIME_ATTR);
            } else if (qName.equals("time") && datatype == null) {
                datatype = AUTODETECT_DATE_DATATYPE;
            }
        }

        String dt = current.resolvePredOrDatatype(datatype, dh.rdfaVersion);
        if (dt == null && datatype != null && datatype.length() > 0) {
            datatype = null; // ignore incorrect datatype
        }

        if (datatype != null && datatype.length() > 0 && !RDF.XML_LITERAL.equals(dt)) {
            // RDFa Core 1.0 processing sequence step 9, typed literal case
            // RDFa Core 1.1 processing sequence step 11, typed literal case
            if (content != null) {
                try {
                    objectLit = TypedLiteral.from(content, dt);
                } catch (ParseException e) {
                    objectLit = new PlainLiteral(content, current.lang);
                }
            } else {
                current.objectLitDt = dt;
            }
        } else if (dh.rdfaVersion > RDFa.VERSION_10) {
            if (datatype != null) {
                if (datatype.length() == 0) {
                    // RDFa Core 1.1 processing sequence step 11, plain literal case
                    if (content != null) {
                        objectLit = new PlainLiteral(content, current.lang);
                    } else {
                        current.objectLitDt = PLAIN_LITERAL;
                    }
                } else if (datatype.length() > 0) { // == rdf:XMLLiteral
                    // RDFa Core 1.1 processing sequence step 11, xml literal case
                    current.objectLitDt = RDF.XML_LITERAL;
                    recurse = false;
                }
            } else {
                if (content != null) {
                    // RDFa Core 1.1 processing sequence step 11, plain literal using @content case
                    objectLit = new PlainLiteral(content, current.lang);
                } else if (attrs.getValue(RDFa.CONTENT_ATTR) == null
                        && attrs.getValue(VALUE_ATTR) == null
                        && noRelsAndRevs) {
                    // RDFa Core 1.1 processing sequence step 11, no rel or rev or content case
                    try {
                        objectNonLit = coalesce(qName, attrs, parent, current,
                                RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                    } catch (ParseException e) {
                        warning(RDFa.WARNING);
                        pushCurrentContext(current, parent);
                    }
                }
                if (objectLit == null && objectNonLit == null
                        && attrs.getValue(RDFa.ABOUT_ATTR) == null
                        && attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                    // RDFa Core 1.1 processing sequence step 11, @typeof present and @about is not case
                    objectNonLit = current.object;
                }
                if (objectLit == null && objectNonLit == null) {
                    // RDFa Core 1.1 processing sequence step 11, last plain literal case
                    current.objectLitDt = PLAIN_LITERAL;
                }
            }
        } else {
            if (content != null) {
                // RDFa Core 1.0 processing sequence step 9, plain literal case
                objectLit = new PlainLiteral(content, current.lang);
            } else {
                if (datatype == null || datatype.length() > 0) {
                    // RDFa Core 1.0 processing sequence step 9, xml literal case
                    current.objectLitDt = RDF.XML_LITERAL;
                    recurse = false;
                } else {
                    // RDFa Core 1.0 processing sequence step 9, plain literal case
                    current.objectLitDt = PLAIN_LITERAL;
                }
            }
        }

        String[] parts = attrs.getValue(RDFa.PROPERTY_ATTR).trim().split(SEPARATOR);
        for (String pred : parts) {
            String iri = current.resolvePredOrDatatype(pred, dh.rdfaVersion);
            if (iri == null) {
                continue;
            }
            if (objectLit != null || objectNonLit != null) {
                if (dh.rdfaVersion > RDFa.VERSION_10 && attrs.getValue(RDFa.INLIST_ATTR) != null) {
                    List<Object> list = current.getMappingForIri(iri);
                    if (objectLit != null) {
                        list.add(objectLit);
                    } else {
                        list.add(objectNonLit);
                    }
                } else {
                    if (objectLit != null) {
                        addLiteralTriple(current.subject, iri, objectLit);
                    } else {
                        addNonLiteral(current.subject, iri, objectNonLit);
                    }
                }
            } else if (current.properties == null) {
                if (dh.rdfaVersion > RDFa.VERSION_10 && attrs.getValue(RDFa.INLIST_ATTR) != null) {
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
        return recurse;
    }

    private void pushContext(EvalContext current, EvalContext parent, boolean skipElement, boolean recurse) {
        current.parsingLiteral = !recurse;
        if (!skipElement && current.subject != null) {
            // RDFa Core 1.0 processing sequence step 10
            // RDFa Core 1.1 processing sequence step 12
            String subject = parent.subject;
            for (Object obj : parent.incomplTriples) {
                if (obj instanceof String) {
                    String pred = (String) obj;
                    if (pred.startsWith(FORWARD)) {
                        addNonLiteral(subject, pred.substring(PREFIX_LENGTH), current.subject);
                    } else {
                        addNonLiteral(current.subject, pred.substring(PREFIX_LENGTH), subject);
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
        // RDFa Core 1.0 processing sequence step 11
        // RDFa Core 1.1 processing sequence step 13
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
            pushCurrentContext(current, parent);
        }
    }

    private void pushCurrentContext(EvalContext current, EvalContext parent) {
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

    @Override
    public void endElement(String nsUri, String localName, String qName) throws SAXException {
        if (rdfXmlInline) {
            if (dh.documentFormat == DocumentContext.FORMAT_SVG && localName.equals(METADATA)) {
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
            if (dh.rdfaVersion == RDFa.VERSION_10 && !xmlString.contains("<")) {
                for (String pred : xmlStringPred.split(SEPARATOR)) {
                    addPlainLiteral(xmlStringSubj, pred, xmlString, current.lang);  // TODO: RDFa 1.0 case 212
                }
            } else {
                for (String pred : xmlStringPred.split(SEPARATOR)) {
                    addTypedLiteral(xmlStringSubj, pred, xmlString, RDF.XML_LITERAL);
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
            processContent(content, current, parent);
        }

        if (parent.listMapping == current.listMapping) {
            return;
        }

        Map<String, List<Object>> list = current.listMapping;
        if (sinkOutputGraph) {
            // RDFa Core 1.0 processing sequence step 14
            for (String pred : list.keySet()) {
                String prev = null;
                String start = null;
                for (Object res : list.get(pred)) {
                    String child = dh.createBnode();
                    if (res instanceof LiteralNode) {
                        addLiteralTriple(child, RDF.FIRST, (LiteralNode) res);
                    } else {
                        addNonLiteral(child, RDF.FIRST, (String) res);
                    }
                    if (prev == null) {
                        start = child;
                    } else {
                        addNonLiteral(prev, RDF.REST, child);
                    }
                    prev = child;
                }
                if (start == null) {
                    addIriRef(current.subject, pred, RDF.NIL);
                } else {
                    addIriRef(prev, RDF.REST, RDF.NIL);
                    addNonLiteral(current.subject, pred, start);
                }
                list.remove(pred);
            }
        } else {
            list.clear();
        }
    }

    private void processContent(String content, EvalContext current, EvalContext parent) {
        if (!parent.parsingLiteral && parent.objectLit != null) {
            parent.objectLit += content;
        } else {
            String dt = current.objectLitDt;
            boolean inlist = current.properties.startsWith(RDFa.INLIST_ATTR + " ");

            if (inlist) {
                LiteralNode currObjectLit;
                if (dt.isEmpty()) { // plain
                    currObjectLit = new PlainLiteral(content, current.lang);
                } else {
                    try {
                        currObjectLit = TypedLiteral.from(content, dt);
                    } catch (ParseException e) {
                        currObjectLit = new PlainLiteral(content, current.lang);
                    }
                }
                for (String predIri : current.properties.substring(
                        RDFa.INLIST_ATTR.length() + 1).split(SEPARATOR)) {
                    current.getMappingForIri(predIri).add(currObjectLit);
                }
            } else {
                if (dt.isEmpty()) {// plain
                    for (String predIri : current.properties.split(SEPARATOR)) {
                        addPlainLiteral(current.subject, predIri, content, current.lang);
                    }
                } else {
                    for (String predIri : current.properties.split(SEPARATOR)) {
                        try {
                            TypedLiteral lit = TypedLiteral.from(content, dt);
                            addTypedLiteral(current.subject, predIri, lit.getContent(), lit.getType());
                        } catch (ParseException e) {
                            addPlainLiteral(current.subject, predIri, content, current.lang);
                        }
                    }
                }
            }
        }
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
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void startDTD(String s, String s1, String s2) throws SAXException {
        dh.processDtd(s, s1, s2);
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
    public void endPrefixMapping(String prefix) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.endPrefixMapping(prefix);
        }
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
        dh.base = baseUri;
    }

    @Override
    public RdfaParser streamingTo(TripleSink sink) {
        this.sink = sink;
        return this;
    }

    @Override
    public String resolveBNode(String value) {
        return dh.resolveBNode(value);
    }

    @Override
    public String resolveIri(String iri) throws MalformedIRIException {
        return dh.resolveIri(iri);
    }

    @Override
    public void declareVocabulary(String vocab) {
        if (sinkOutputGraph) {
            sink.addIriRef(dh.base, RDFa.USES_VOCABULARY, vocab);
        }
    }

    // error handling

    @Override
    public void warning(String warningClass) {
        if (dh.rdfaVersion > RDFa.VERSION_10 && sinkProcessorGraph) {
            sink.addIriRef(dh.createBnode(), RDF.TYPE, warningClass);
        }
    }

    @Override
    public void error(String errorClass) {
        if (dh.rdfaVersion > RDFa.VERSION_10 && sinkProcessorGraph) {
            sink.addIriRef(dh.createBnode(), RDF.TYPE, errorClass);
        }
    }

    @Override
    public ParseException processException(SAXException e) {
        error(RDFa.ERROR);
        Throwable cause = e.getCause();
        if (cause instanceof ParseException) {
            return (ParseException) cause;
        }
        return new ParseException(e);
    }

    // proxying TripleSink calls to filter output graph

    private void addLiteralTriple(String subject, String pred, LiteralNode literal) {
        if (literal instanceof TypedLiteral) {
            TypedLiteral tl = (TypedLiteral) literal;
            addTypedLiteral(subject, pred, tl.getContent(), tl.getType());
        } else {
            PlainLiteral pl = (PlainLiteral) literal;
            addPlainLiteral(subject, pred, pl.getContent(), pl.getLang());
        }
    }

    @Override
    public void addNonLiteral(String subj, String pred, String obj) {
        if (sinkOutputGraph) {
            sink.addNonLiteral(subj, pred, obj);
        }
    }

    @Override
    public void addIriRef(String subj, String pred, String iri) {
        if (sinkOutputGraph) {
            sink.addIriRef(subj, pred, iri);
        }
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        if (sinkOutputGraph) {
            sink.addPlainLiteral(subj, pred, content, lang);
        }
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        if (sinkOutputGraph) {
            sink.addTypedLiteral(subj, pred, content, type);
        }
    }

    // ignored events

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
    public void endDTD() throws SAXException {
    }

}
