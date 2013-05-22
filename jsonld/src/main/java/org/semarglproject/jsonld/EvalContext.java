/**
 * Copyright 2012-2013 the Semargl contributors. See AUTHORS for more details.
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
package org.semarglproject.jsonld;

import org.semarglproject.ri.MalformedCurieException;
import org.semarglproject.ri.MalformedIriException;
import org.semarglproject.ri.RIUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

final class EvalContext {

    private static final Pattern TERM_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+", Pattern.DOTALL);

    private static final String CAN_NOT_RESOLVE_TERM = "Can't resolve term ";

    String graph;
    String subject;
    String predicate;
    String lang;
    String objectLit;
    String objectLitDt;
    String listTail;
    boolean parsingContext;
    boolean parsingArray;

    private final DocumentContext documentContext;
    private final Map<String, String> iriMappings;
    private final Map<String, String> dtMappings;
    private final Map<String, String> terms;

    private EvalContext(DocumentContext documentContext) {
        iriMappings = new TreeMap<String, String>();
        dtMappings = new TreeMap<String, String>();
        terms = new HashMap<String, String>();
        this.documentContext = documentContext;
    }

    static EvalContext createInitialContext(DocumentContext documentContext) {
        EvalContext initialContext = new EvalContext(documentContext);
        initialContext.subject = documentContext.base;
        return initialContext;
    }

    EvalContext initChildContext() {
        EvalContext child = new EvalContext(documentContext);
        child.subject = documentContext.createBnode(false);
        child.iriMappings.putAll(iriMappings);
        child.dtMappings.putAll(dtMappings);
        child.terms.putAll(terms);
        child.parsingContext = this.parsingContext;
        child.lang = this.lang;
        return child;
    }

    /**
     * Resolves @predicate or @datatype
     *
     * @param value value of attribute
     * @return resource IRI
     * @throws org.semarglproject.ri.MalformedIriException if IRI can not be resolved
     */
    String resolvePredOrDatatype(String value) throws MalformedIriException {
        if (value == null || value.isEmpty()) {
            throw new MalformedIriException("Empty predicate or datatype found");
        }
        return resolveTermOrCurieOrAbsIri(value);
    }

    /**
     * Resolves @about or @resource
     *
     * @param value value of attribute
     * @return resource IRI
     * @throws org.semarglproject.ri.MalformedIriException if IRI can not be resolved
     */
    String resolveAboutOrResource(String value) throws MalformedIriException {
        String result = documentContext.resolveBNode(value);
        if (result != null) {
            return result;
        }
        return resolveCurieOrIri(value, false);
    }

    /**
     * Resolves TERMorCURIEorAbsIRI
     * @param value value to be resolved
     * @return resource IRI
     * @throws org.semarglproject.ri.MalformedIriException if IRI can not be resolved
     */
    private String resolveTermOrCurieOrAbsIri(String value) throws MalformedIriException {
        if (TERM_PATTERN.matcher(value).matches()) {
            if (terms.containsKey(value)) {
                return terms.get(value);
            } else {
                throw new MalformedIriException(CAN_NOT_RESOLVE_TERM + value);
            }
        }
        try {
            return resolveCurieOrIri(value, true);
        } catch (MalformedCurieException e) {
            throw new MalformedIriException(e.getMessage());
        }
    }

    private String resolveCurieOrIri(String curie, boolean ignoreRelIri) throws MalformedIriException {
        if (!ignoreRelIri && (curie == null || curie.isEmpty())) {
            return documentContext.resolveIri(curie);
        }

        int delimPos = curie.indexOf(':');
        if (delimPos == -1) {
            if (ignoreRelIri) {
                throw new MalformedCurieException("CURIE with no prefix (" + curie + ") found");
            }
            return documentContext.resolveIri(curie);
        }

        String result = resolveMapping(curie, delimPos);
        if (RIUtils.isIri(result)) {
            return result;
        }
        throw new MalformedIriException("Malformed IRI: " + curie);
    }

    private String resolveMapping(String curie, int delimPos) throws MalformedCurieException {
        String localName = curie.substring(delimPos + 1);
        String prefix = curie.substring(0, delimPos);

        if (prefix.equals("_")) {
            throw new MalformedCurieException("CURIE with invalid prefix (" + curie + ") found");
        }

        if (!iriMappings.containsKey(prefix)) {
            if (RIUtils.isIri(curie)) {
                return curie;
            }
            throw new MalformedCurieException("CURIE with unresolvable prefix found (" + curie + ")");
        }
        return iriMappings.get(prefix) + localName;
    }

    void setPredicate(String predicate) {
        if (predicate.charAt(0) != '@') {
            try {
                this.predicate = resolvePredOrDatatype(predicate);
            } catch (MalformedIriException e) {
                if (!parsingContext) {
                    if (iriMappings.containsKey(predicate)) {
                        this.predicate = iriMappings.get(predicate);
                    } else {
                        this.predicate = null;
                    }
                } else {
                    this.predicate = predicate;
                }
            }
        } else {
            this.predicate = predicate;
        }
    }

    boolean isPredicateValid() {
        return predicate != null && predicate.charAt(0) != '@';
    }

    boolean isPredicateKeyword() {
        return predicate.charAt(0) == '@';
    }

    void setPredicateDtMapping(String value) {
        try {
            if (value.charAt(0) != '@') {
                value = resolvePredOrDatatype(value);
            }
            dtMappings.put(predicate, value);
        } catch (MalformedIriException e) {
        }
    }

    String getPredicateDtMapping() {
        if (dtMappings.containsKey(predicate)) {
            return dtMappings.get(predicate);
        }
        return null;
    }

    void defineTerm(String uri) {
        terms.put(predicate, uri);
        predicate = uri;
    }

    void addIriMapping(String key, String iri) {
        iriMappings.put(key, iri);
    }
}
