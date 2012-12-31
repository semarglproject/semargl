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
import org.semarglproject.rdf.ProcessorGraphHandler;
import org.semarglproject.rdf.RdfXmlParser;
import org.semarglproject.ri.MalformedCurieException;
import org.semarglproject.ri.MalformedIriException;
import org.semarglproject.ri.RIUtils;
import org.semarglproject.sink.Converter;
import org.semarglproject.sink.SaxSink;
import org.semarglproject.sink.TripleSink;
import org.semarglproject.vocab.RDF;
import org.semarglproject.vocab.RDFa;
import org.semarglproject.xml.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of RDFa (1.0 and 1.1) parser.
 * List of supported options
 * <ul>
 *     <li>{@link #RDFA_VERSION_PROPERTY}</li>
 *     <li>{@link #PROCESSOR_GRAPH_HANDLER_PROPERTY}</li>
 *     <li>{@link #ENABLE_OUTPUT_GRAPH}</li>
 *     <li>{@link #ENABLE_PROCESSOR_GRAPH}</li>
 *     <li>{@link #ENABLE_VOCAB_EXPANSION}</li>
 * </ul>
 */
public final class RdfaParser extends Converter<SaxSink, TripleSink>
        implements SaxSink, TripleSink, ProcessorGraphHandler {

    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * RDFa version compatibility. Allowed values are {@link RDFa#VERSION_10} and {@link RDFa#VERSION_11}.
     */
    public static final String RDFA_VERSION_PROPERTY =
            "http://semarglproject.org/rdfa/properties/version";
    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Allows to specify handler for processor graph events.
     * Subclass of {@link ProcessorGraphHandler} must be passed as value.
     */
    public static final String PROCESSOR_GRAPH_HANDLER_PROPERTY =
            "http://semarglproject.org/rdfa/properties/processor-graph-handler";
    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Enables or disables generation of triples from output graph.
     */
    public static final String ENABLE_OUTPUT_GRAPH =
            "http://semarglproject.org/rdfa/properties/enable-output-graph";
    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Enables or disables generation of triples from processor graph.
     * ProcessorGraphHandler will receive events regardless of this option.
     */
    public static final String ENABLE_PROCESSOR_GRAPH =
            "http://semarglproject.org/rdfa/properties/enable-processor-graph";
    /**
     * Used as a key with {@link #setProperty(String, Object)} method.
     * Enables or disables vocabulary expansion feature.
     */
    public static final String ENABLE_VOCAB_EXPANSION =
            "http://semarglproject.org/rdfa/properties/enable-vocab-expansion";

    static final String AUTODETECT_DATE_DATATYPE = "AUTODETECT_DATE_DATATYPE";

    private static final ThreadLocal<VocabManager> VOCAB_MANAGER = new ThreadLocal<VocabManager>() {
        @Override
        protected VocabManager initialValue() {
            return new VocabManager();
        }
    };

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

    // keys for coalesce method
    private static final String BASE_IF_HEAD_OR_BODY = "bihob";
    private static final String BASE_IF_ROOT_NODE = "birn";
    private static final String PARENT_OBJECT = "poie";
    private static final String BNODE_IF_TYPEOF = RDFa.TYPEOF_ATTR;

    private Deque<EvalContext> contextStack = null;

    private String xmlString = null;
    private String xmlStringPred = null;
    private String xmlStringSubj = null;

    private short defaultRdfaVersion = RDFa.VERSION_11;
    private boolean sinkOutputGraph;
    private boolean sinkProcessorGraph;

    private boolean expandVocab;
    private DocumentContext dh;
    private Locator locator;
    private ProcessorGraphHandler processorGraphHandler;

    private boolean rdfXmlInline = false;
    private SaxSink rdfXmlParser = null;

    private final Map<String, String> overwriteMappings = new HashMap<String, String>();

    private RdfaParser() {
        contextStack = new LinkedList<EvalContext>();
        dh = new DocumentContext(null, RDFa.VERSION_11, this);
        sinkProcessorGraph = true;
        sinkOutputGraph = true;
        expandVocab = false;
    }

    /**
     * Creates instance of RdfaParser connected to specified sink
     * @param sink sink to be connected to
     * @return instance of RdfaParser
     */
    public static SaxSink connect(TripleSink sink) {
        RdfaParser parser = new RdfaParser();
        parser.sink = sink;
        return parser;
    }

    @Override
    public void startDocument() {
        EvalContext initialContext = EvalContext.createInitialContext(dh);
        initialContext.iriMappings.put("", XHTML_VOCAB);
        contextStack.push(initialContext);

        xmlString = null;
        xmlStringPred = null;
        xmlStringSubj = null;

        rdfXmlInline = false;
        rdfXmlParser = null;
    }

    @Override
    public void endDocument() throws SAXException {
        dh.clear(defaultRdfaVersion);
        contextStack.clear();
    }

    @Override
    public void startElement(String nsUri, String localName, String qName, Attributes attrs) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.startElement(nsUri, localName, qName, attrs);
            return;
        } else if (dh.documentFormat == DocumentContext.FORMAT_SVG && localName.equals(METADATA)) {
            if (rdfXmlParser == null) {
                rdfXmlParser = RdfXmlParser.connect(this);
                rdfXmlParser.setBaseUri(dh.base);
                rdfXmlParser.startDocument();
            }
            rdfXmlInline = true;
            return;
        }

        if (contextStack.size() < 4) {
            String oldBase = dh.base;
            dh.detectFormat(localName, qName, attrs.getValue(VERSION));
            dh.detectBase(qName, attrs.getValue(XML_BASE), attrs.getValue(RDFa.HREF_ATTR));
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
                attrs.getValue(RDFa.VOCAB_ATTR), lang, overwriteMappings);
        overwriteMappings.clear();

        boolean skipTerms = dh.rdfaVersion > RDFa.VERSION_10 && dh.isHtmlDocument()
                && attrs.getValue(RDFa.PROPERTY_ATTR) != null;
        List<String> rels = convertRelRevToList(attrs.getValue(RDFa.REL_ATTR), skipTerms);
        List<String> revs = convertRelRevToList(attrs.getValue(RDFa.REV_ATTR), skipTerms);
        boolean noRelsAndRevs = rels == null && revs == null;

        boolean skipElement = findSubjectAndObject(qName, attrs, noRelsAndRevs, current, parent);

        // don't fill parent list if subject was changed at this
        // or previous step by current.parentObject
        if (dh.rdfaVersion > RDFa.VERSION_10 && current.subject != null
                && (current.subject != parent.object || parent.object != parent.subject)) {
            // RDFa Core 1.1 processing sequence step 8
            current.listMapping = new HashMap<String, List<Object>>();
        }

        processRels(attrs, rels, current);
        processRevs(revs, current);

        if (current.object == null && !noRelsAndRevs) {
            current.object = dh.createBnode(false);
        }

        processPropertyAttr(qName, attrs, current, parent, noRelsAndRevs);

        if (dh.rdfaVersion > RDFa.VERSION_10) {
            processRoleAttribute(attrs.getValue(RDFa.ID_ATTR), attrs.getValue(RDFa.ROLE_ATTR), current);
        }

        if (!skipElement) {
            // RDFa Core 1.0 processing sequence step 10
            // RDFa Core 1.1 processing sequence step 12
            processIncompleteTriples(current, parent);
        }

        // RDFa Core 1.0 processing sequence step 11
        // RDFa Core 1.1 processing sequence step 13
        pushContext(current, parent, skipElement);
    }

    /**
     * Splits @rel or @rev attribute value to list of predicates. Terms can be optionally ignored.
     * @param propertyVal value of @rel or @rev attribute
     * @param skipTerms is terms should be skipped
     * @return list of predicates
     */
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

    /**
     * Generates triples related to @role attribute
     * @param id value of @id attribute
     * @param roleVal value of @role attribute
     * @param current current context
     */
    private void processRoleAttribute(String id, String roleVal, EvalContext current) {
        if (roleVal == null) {
            return;
        }
        List<String> roles = new ArrayList<String>();
        for (String role : roleVal.split(SEPARATOR)) {
            try {
                roles.add(current.resolveRole(role));
            } catch (MalformedIriException e) {
                // do nothing
            }
        }
        String subject;
        if (id != null) {
            subject = dh.base + '#' + id;
        } else {
            subject = dh.createBnode(true);
        }
        for (String role : roles) {
            addNonLiteral(subject, XHTML_VOCAB + "role", role);
        }
    }

    /**
     * Determines object and subject for current context
     * @param qName node's qName
     * @param attrs node's attributes
     * @param noRelAndRev is no @rel and @rev attributes specified
     * @param current current context
     * @param parent parent context
     * @return skip element flag
     */
    private boolean findSubjectAndObject(String qName, Attributes attrs, boolean noRelAndRev, EvalContext current,
                                         EvalContext parent) {
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
                                    current.object = dh.createBnode(noRelAndRev);
                                }
                            }
                            newSubject = current.object;
                        }
                    } else {
                        // RDFa Core 1.1 processing sequence step 5.2
                        current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                                RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR, BASE_IF_ROOT_NODE,
                                BNODE_IF_TYPEOF, PARENT_OBJECT);
                        if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                            newSubject = current.subject;
                        }
                    }
                } else {
                    // RDFa Core 1.1 processing sequence step 6
                    current.object = coalesce(qName, attrs, parent, current, RDFa.RESOURCE_ATTR, DATA_ATTR,
                            RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                    current.subject = coalesce(qName, attrs, parent, current, RDFa.ABOUT_ATTR,
                            BASE_IF_ROOT_NODE, PARENT_OBJECT);
                    if (attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                        if (attrs.getValue(RDFa.ABOUT_ATTR) != null) {
                            newSubject = current.subject;
                        } else {
                            if (current.object == null) {
                                current.object = dh.createBnode(noRelAndRev);
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
        }  catch (MalformedIriException e) {
            warning(RDFa.WARNING, e.getMessage());
            pushContextNoLiteral(current, parent);
        }

        if (newSubject != null) {
            // RDFa Core 1.0 processing sequence step 6
            // RDFa Core 1.1 processing sequence step 7
            for (String type : attrs.getValue(RDFa.TYPEOF_ATTR).split(SEPARATOR)) {
                try {
                    String iri = current.resolvePredOrDatatype(type);
                    addNonLiteral(newSubject, RDF.TYPE, iri);
                } catch (MalformedIriException e) {
                    // do nothing
                }
            }
        }
        return noRelAndRev && current.subject == parent.object && attrs.getValue(RDFa.PROPERTY_ATTR) == null;
    }

    /**
     * Iterates through attribute names list and returns first not null
     * value of attribute with such name. Also processes special cases
     * if no such attributes found:
     * <ul>
     *     <li>{@link #BNODE_IF_TYPEOF} - returns new bnode if typeof attr found</li>
     *     <li>{@link #PARENT_OBJECT} - returns parent.object</li>
     *     <li>{@link #BASE_IF_HEAD_OR_BODY} - returns base if processing head or body node in HTML</li>
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
                            EvalContext current, String... attrNames) throws MalformedIriException {
        for (String attr : attrNames) {
            if (attrs.getValue(attr) != null) {
                if (attr.equals(RDFa.ABOUT_ATTR) || attr.equals(RDFa.RESOURCE_ATTR)) {
                    String val = attrs.getValue(attr);
                    if (val.equals("[]")) {
                        continue;
                    }
                    try {
                        return current.resolveAboutOrResource(val);
                    } catch (MalformedCurieException e) {
                        warning(RDFa.UNRESOLVED_CURIE, e.getMessage());
                        return null;
                    }
                } else if (attr.equals(RDFa.HREF_ATTR) || attr.equals(RDFa.SRC_ATTR) || attr.equals(DATA_ATTR)) {
                    return dh.resolveIri(attrs.getValue(attr));
                } else if (attr.equals(BNODE_IF_TYPEOF)) {
                    return dh.createBnode(false);
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

    /**
     * Generates [incompleted] triples with predicates from @rel attribute
     * @param attrs node's attributes
     * @param rels list of predicates from @rel attribute
     * @param current current context
     */
    private void processRels(Attributes attrs, List<String> rels, EvalContext current) {
        if (rels != null) {
            boolean inList = dh.rdfaVersion > RDFa.VERSION_10 && attrs.getValue(RDFa.INLIST_ATTR) != null;
            // RDFa Core 1.1 processing sequence steps 9 and 10
            // RDFa Core 1.0 processing sequence steps 7 and 8
            for (String predicate : rels) {
                String iri;
                try {
                    iri = current.resolvePredOrDatatype(predicate);
                } catch (MalformedIriException e) {
                    continue;
                }
                if (inList) {
                    List<Object> list = current.getMappingForIri(iri);
                    if (current.object != null) {
                        list.add(current.object);
                    } else {
                        current.incomplTriples.add(list);
                    }
                } else {
                    if (current.object != null) {
                        addNonLiteral(current.subject, iri, current.object);
                    } else {
                        current.incomplTriples.add(FORWARD + iri);
                    }
                }
            }
        }
    }

    /**
     * Generates [incompleted] triples with predicates from @rev attribute
     * @param revs list of predicates from @rev attribute
     * @param current current context
     */
    private void processRevs(List<String> revs, EvalContext current) {
        if (revs != null) {
            for (String predicate : revs) {
                // RDFa Core 1.1 processing sequence steps 9 and 10
                try {
                    String iri = current.resolvePredOrDatatype(predicate);
                    if (current.object != null) {
                        addNonLiteral(current.object, iri, current.subject);
                    } else {
                        current.incomplTriples.add(REVERSE + iri);
                    }
                } catch (MalformedIriException e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Processes @property attribute of specified node
     * @param qName node's QName
     * @param attrs node's attributes
     * @param current current context
     * @param parent parent context
     * @param noRelsAndRevs are on @rel and @rev attributes specified
     */
    private void processPropertyAttr(String qName, Attributes attrs, EvalContext current,
                                     EvalContext parent, boolean noRelsAndRevs) {
        if (attrs.getValue(RDFa.PROPERTY_ATTR) == null) {
            current.parsingLiteral = false;
            return;
        }

        // RDFa Core 1.0 processing sequence step 9
        // RDFa Core 1.1 processing sequence step 11
        Object object = parseLiteralObject(qName, attrs, current, parent, noRelsAndRevs);

        boolean inList = attrs.getValue(RDFa.INLIST_ATTR) != null;
        for (String pred : attrs.getValue(RDFa.PROPERTY_ATTR).trim().split(SEPARATOR)) {
            processPropertyPredicate(pred, object, current, inList);
        }

        current.parsingLiteral = current.objectLitDt == RDF.XML_LITERAL;
        if (current.properties == null) {
            current.objectLitDt = null;
            current.parsingLiteral = false;
        }
    }

    /**
     * Determines literal object for specified node. Can change objectLitDt in current context
     * @param qName node's QName
     * @param attrs node's attributes
     * @param current current context
     * @param parent parent context
     * @param noRelsAndRevs are on @rel and @rev attributes specified
     * @return object found or null otherwise
     */
    private Object parseLiteralObject(String qName, Attributes attrs, EvalContext current,
                                      EvalContext parent, boolean noRelsAndRevs) {
        String content = parseContent(attrs);
        String datatype = parseDatatype(qName, attrs, current);

        if (datatype != null && !RDF.XML_LITERAL.equals(datatype)) {
            // RDFa Core 1.0 processing sequence step 9, typed literal case
            // RDFa Core 1.1 processing sequence step 11, typed literal case
            if (content != null) {
                try {
                    return TypedLiteral.from(content, datatype);
                } catch (ParseException e) {
                    return new PlainLiteral(content, current.lang);
                }
            } else {
                current.objectLitDt = datatype;
            }
        } else if (dh.rdfaVersion > RDFa.VERSION_10) {
            if (datatype != null) {
                if (datatype.length() == 0) {
                    // RDFa Core 1.1 processing sequence step 11, plain literal case
                    if (content != null) {
                        return new PlainLiteral(content, current.lang);
                    } else {
                        current.objectLitDt = PLAIN_LITERAL;
                    }
                } else if (datatype.length() > 0) {
                    // == rdf:XMLLiteral
                    // RDFa Core 1.1 processing sequence step 11, xml literal case
                    current.objectLitDt = RDF.XML_LITERAL;
                }
            } else {
                Object result = null;
                if (content != null) {
                    // RDFa Core 1.1 processing sequence step 11, plain literal using @content case
                    return new PlainLiteral(content, current.lang);
                } else if (attrs.getValue(RDFa.CONTENT_ATTR) == null
                        && attrs.getValue(VALUE_ATTR) == null
                        && noRelsAndRevs) {
                    // RDFa Core 1.1 processing sequence step 11, no rel or rev or content case
                    try {
                        result = coalesce(qName, attrs, parent, current,
                                RDFa.RESOURCE_ATTR, DATA_ATTR, RDFa.HREF_ATTR, RDFa.SRC_ATTR);
                    } catch (MalformedIriException e) {
                        warning(RDFa.WARNING, e.getMessage());
                        pushContextNoLiteral(current, parent);
                    }
                }
                if (result == null && attrs.getValue(RDFa.ABOUT_ATTR) == null
                        && attrs.getValue(RDFa.TYPEOF_ATTR) != null) {
                    // RDFa Core 1.1 processing sequence step 11, @typeof present and @about is not case
                    result = current.object;
                }
                if (result == null) {
                    // RDFa Core 1.1 processing sequence step 11, last plain literal case
                    current.objectLitDt = PLAIN_LITERAL;
                }
                return result;
            }
        } else {
            if (content != null) {
                // RDFa Core 1.0 processing sequence step 9, plain literal case
                return new PlainLiteral(content, current.lang);
            } else {
                if (datatype == null || datatype.length() > 0) {
                    // RDFa Core 1.0 processing sequence step 9, xml literal case
                    current.objectLitDt = RDF.XML_LITERAL;
                } else {
                    // RDFa Core 1.0 processing sequence step 9, plain literal case
                    current.objectLitDt = PLAIN_LITERAL;
                }
            }
        }
        return null;
    }

    /**
     * Extracts content for specified node with respect of HTML5 attributes
     * @param attrs node's attributes
     * @return content
     */
    private String parseContent(Attributes attrs) {
        String content = attrs.getValue(RDFa.CONTENT_ATTR);
        if (dh.documentFormat == DocumentContext.FORMAT_HTML5) {
            if (attrs.getValue(VALUE_ATTR) != null) {
                content = attrs.getValue(VALUE_ATTR);
            }
            if (attrs.getValue(DATETIME_ATTR) != null) {
                content = attrs.getValue(DATETIME_ATTR);
            }
        }
        return content;
    }

    /**
     * Extracts datatype uri for specified node
     * @param qName node's QName
     * @param attrs node's attributes
     * @param current current context
     * @return datatype URI or {@link #AUTODETECT_DATE_DATATYPE} if datatype should be detected at validation phase
     */
    private String parseDatatype(String qName, Attributes attrs, EvalContext current) {
        String datatype = attrs.getValue(RDFa.DATATYPE_ATTR);
        if (dh.documentFormat == DocumentContext.FORMAT_HTML5) {
            if (attrs.getValue(DATETIME_ATTR) != null) {
                if (datatype == null) {
                    datatype = AUTODETECT_DATE_DATATYPE;
                }
            } else if (qName.equals("time") && datatype == null) {
                datatype = AUTODETECT_DATE_DATATYPE;
            }
        }
        try {
            if (datatype != null && datatype.length() > 0) {
                datatype = current.resolvePredOrDatatype(datatype);
            }
        } catch (MalformedIriException e) {
            datatype = null;
        }
        return datatype;
    }

    /**
     * Generates triples corresponding to specified object and predicate from @property
     * @param pred predicate from property attribute
     * @param object statement object can be LiteralNode or String (URI or BNode)
     * @param current current context
     * @param inList is inlist property presented
     */
    private void processPropertyPredicate(String pred, Object object, EvalContext current, boolean inList) {
        String iri;
        try {
            iri = current.resolvePredOrDatatype(pred);
        } catch (MalformedIriException e) {
            return;
        }
        if (object != null) {
            if (dh.rdfaVersion > RDFa.VERSION_10 && inList) {
                List<Object> list = current.getMappingForIri(iri);
                list.add(object);
            } else {
                if (object instanceof LiteralNode) {
                    addLiteralTriple(current.subject, iri, (LiteralNode) object);
                } else {
                    addNonLiteral(current.subject, iri, (String) object);
                }
            }
        } else if (current.properties == null) {
            if (dh.rdfaVersion > RDFa.VERSION_10 && inList) {
                current.properties = RDFa.INLIST_ATTR + " " + iri;
            } else {
                current.properties = iri;
            }
        } else {
            current.properties += " " + iri;
        }
    }

    /**
     * Generates triples from parent's incompleted triples list
     * @param current current context
     * @param parent parent context
     */
    private void processIncompleteTriples(EvalContext current, EvalContext parent) {
        if (current.subject == null) {
            return;
        }
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

    /**
     * Pushes current context to stack before processing child nodes
     * @param current current context
     * @param parent parent context
     */
    private void pushContext(EvalContext current, EvalContext parent, boolean skipElement) {
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
            pushContextNoLiteral(current, parent);
        }
    }

    /**
     * Pushes current context to stack before processing child nodes when no literals are parsed
     * @param current current context
     * @param parent parent context
     */
    private void pushContextNoLiteral(EvalContext current, EvalContext parent) {
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
            // delegate parsing to RDF/XML parser
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
        processXmlString(current);

        // serialize close tag if parsing literal
        if (xmlString != null) {
            xmlString += "</" + qName + ">";
        }

        if (contextStack.isEmpty()) {
            return;
        }

        EvalContext parent = contextStack.peek();
        processContent(current, parent);

        if (parent.listMapping != current.listMapping) {
            // RDFa Core 1.0 processing sequence step 14
            processListMappings(current);
        }
    }

    /**
     * Generates triples for parsed literal if it present
     * @param current current context
     */
    private void processXmlString(EvalContext current) {
        if (current.parsingLiteral && xmlString != null) {
            if (dh.rdfaVersion == RDFa.VERSION_10 && !xmlString.contains("<")) {
                for (String pred : xmlStringPred.split(SEPARATOR)) {
                    addPlainLiteral(xmlStringSubj, pred, xmlString, current.lang);
                }
            } else {
                for (String pred : xmlStringPred.split(SEPARATOR)) {
                    addTypedLiteral(xmlStringSubj, pred, xmlString, RDF.XML_LITERAL);
                }
            }
            xmlString = null;
        }
    }

    /**
     * Generates triples for node content
     * @param current current context
     * @param parent parent context
     */
    private void processContent(EvalContext current, EvalContext parent) {
        String content = current.objectLit;
        if (content == null) {
            return;
        }
        if (!parent.parsingLiteral && parent.objectLit != null) {
            parent.objectLit += content;
        } else {
            String dt = current.objectLitDt;
            boolean inlist = current.properties.startsWith(RDFa.INLIST_ATTR + " ");

            if (inlist) {
                LiteralNode currObjectLit;
                if (dt.isEmpty()) {
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
                if (dt.isEmpty()) {
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

    /**
     * Generates triples from list mappings on node close event
     * @param current current context
     */
    private void processListMappings(EvalContext current) {
        Map<String, List<Object>> list = current.listMapping;
        for (String pred : list.keySet()) {
            String prev = null;
            String start = null;
            for (Object res : list.get(pred)) {
                String child = dh.createBnode(false);
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
                addNonLiteral(current.subject, pred, RDF.NIL);
            } else {
                addNonLiteral(prev, RDF.REST, RDF.NIL);
                addNonLiteral(current.subject, pred, start);
            }
            list.remove(pred);
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
            try {
                overwriteMappings.put(prefix, RIUtils.resolveIri(dh.originUri, uri));
            } catch (MalformedIriException e) {
                // do nothing
            }
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (rdfXmlInline) {
            rdfXmlParser.endPrefixMapping(prefix);
        }
    }

    @Override
    public boolean setProperty(String key, Object value) {
        boolean sinkResult = super.setProperty(key, value);
        if (ENABLE_OUTPUT_GRAPH.equals(key) && value instanceof Boolean) {
            sinkOutputGraph = (Boolean) value;
        } else if (ENABLE_PROCESSOR_GRAPH.equals(key) && value instanceof Boolean) {
            sinkProcessorGraph = (Boolean) value;
        } else if (ENABLE_VOCAB_EXPANSION.equals(key) && value instanceof Boolean) {
            expandVocab = (Boolean) value;
        } else if (sinkProcessorGraph || expandVocab) {
            defaultRdfaVersion = RDFa.VERSION_11;
        } else if (RDFA_VERSION_PROPERTY.equals(key) && value instanceof Integer) {
            int rdfaVersion = (Integer) value;
            if (rdfaVersion < RDFa.VERSION_10 || rdfaVersion > RDFa.VERSION_11) {
                throw new IllegalArgumentException("Unsupported RDFa version");
            }
            defaultRdfaVersion = (short) rdfaVersion;
        } else if (PROCESSOR_GRAPH_HANDLER_PROPERTY.equals(key) && value instanceof ProcessorGraphHandler) {
            processorGraphHandler = (ProcessorGraphHandler) value;
        } else {
            return sinkResult;
        }
        return true;
    }

    @Override
    public void setBaseUri(String baseUri) {
        dh.setBaseUri(baseUri);
    }

    /**
     * Loads vocabulary from specified URL. Vocabulary will not contain terms in case when
     * vocabulary expansion is disabled.
     *
     * @param vocabUrl URL to load from
     * @return loaded vocabulary (can be cached)
     */
    public Vocabulary loadVocabulary(String vocabUrl) {
        if (sinkOutputGraph) {
            sink.addNonLiteral(dh.base, RDFa.USES_VOCABULARY, vocabUrl);
        }
        return VOCAB_MANAGER.get().findVocab(vocabUrl, expandVocab);
    }

    // error handling

    @Override
    public void info(String infoClass, String message) {
        addProcessorGraphRecord(infoClass, message);
        if (processorGraphHandler != null) {
            processorGraphHandler.info(infoClass, message);
        }
    }

    @Override
    public void warning(String warningClass, String message) {
        addProcessorGraphRecord(warningClass, message);
        if (processorGraphHandler != null) {
            processorGraphHandler.warning(warningClass, message);
        }
    }

    @Override
    public void error(String errorClass, String message) {
        addProcessorGraphRecord(errorClass, message);
        if (processorGraphHandler != null) {
            processorGraphHandler.error(errorClass, message);
        }
    }

    private void addProcessorGraphRecord(String recordClass, String recordContext) {
        if (dh.rdfaVersion > RDFa.VERSION_10 && sinkProcessorGraph) {
            String errorNode = dh.createBnode(true);
            String location = "";
            if (locator != null) {
                location = " at " + locator.getLineNumber() + ':' + locator.getColumnNumber();
            }
            sink.addNonLiteral(errorNode, RDF.TYPE, recordClass);
            sink.addPlainLiteral(errorNode, RDFa.CONTEXT, recordContext + location, null);
        }
    }

    @Override
    public ParseException processException(SAXException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ParseException) {
            error(RDFa.ERROR, cause.getMessage());
            return (ParseException) cause;
        }
        error(RDFa.ERROR, e.getMessage());
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
        if (!sinkOutputGraph) {
            return;
        }
        if (!expandVocab) {
            sink.addNonLiteral(subj, pred, obj);
            return;
        }
        addNonLiteralWithObjExpansion(subj, pred, obj);
        for (String predSynonym : contextStack.peek().expand(pred)) {
            addNonLiteralWithObjExpansion(subj, predSynonym, obj);
        }
    }

    private void addNonLiteralWithObjExpansion(String subj, String pred, String obj) {
        if (obj.startsWith(RDF.BNODE_PREFIX)) {
            sink.addNonLiteral(subj, pred, obj);
            return;
        }
        sink.addNonLiteral(subj, pred, obj);
        for (String objSynonym : contextStack.peek().expand(obj)) {
            sink.addNonLiteral(subj, pred, objSynonym);
        }
    }

    @Override
    public void addPlainLiteral(String subj, String pred, String content, String lang) {
        if (!sinkOutputGraph) {
            return;
        }
        sink.addPlainLiteral(subj, pred, content, lang);
        for (String predSynonym : contextStack.peek().expand(pred)) {
            sink.addPlainLiteral(subj, predSynonym, content, lang);
        }
    }

    @Override
    public void addTypedLiteral(String subj, String pred, String content, String type) {
        if (!sinkOutputGraph) {
            return;
        }
        sink.addTypedLiteral(subj, pred, content, type);
        for (String predSynonym : contextStack.peek().expand(pred)) {
            sink.addTypedLiteral(subj, predSynonym, content, type);
        }
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    // ignored events

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
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
