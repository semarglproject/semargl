/**
 * Copyright 2012-2013 Lev Khomich
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
package org.semarglproject.sesame.rdf.rdfa;

import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFParser;

/**
 * Holds configuration of {@link SesameRDFaParser}.
 */
public class RdfaParserConfig extends ParserConfig {

    private final boolean processorGraphEnabled;
    private final boolean vocabExpansionEnabled;
    private final short rdfaCompatibility;

    /**
     * Creates configuration with disabled data verification, enabled stop at first error, enabled preserving
     * of bnode IDs and disabled datatype handling.
     * @param enableProcessorGraph see {@link SesameRDFaParser#setProcessorGraphEnabled(boolean)}
     * @param enableVocabExpansion see {@link SesameRDFaParser#setVocabExpansionEnabled(boolean)}
     * @param rdfaCompatibility see {@link SesameRDFaParser#setRdfaCompatibility(short)}
     */
    public RdfaParserConfig(boolean enableProcessorGraph, boolean enableVocabExpansion, short rdfaCompatibility) {
        super(false, true, true, RDFParser.DatatypeHandling.IGNORE);
        this.processorGraphEnabled = enableProcessorGraph;
        this.vocabExpansionEnabled = enableVocabExpansion;
        this.rdfaCompatibility = rdfaCompatibility;
    }

    /**
     * Creates custom {@link SesameRDFaParser} configuration.
     * @param verifyData see {@link SesameRDFaParser#setVerifyData(boolean)}
     * @param stopAtFirstError see {@link SesameRDFaParser#setStopAtFirstError(boolean)}
     * @param preserveBNodeIDs see {@link SesameRDFaParser#setPreserveBNodeIDs(boolean)}
     * @param dtHandling see {@link SesameRDFaParser#setDatatypeHandling(org.openrdf.rio.RDFParser.DatatypeHandling)}
     * @param enableProcessorGraph see {@link SesameRDFaParser#setProcessorGraphEnabled(boolean)}
     * @param enableVocabExpansion see {@link SesameRDFaParser#setVocabExpansionEnabled(boolean)}
     * @param rdfaCompatibility see {@link SesameRDFaParser#setRdfaCompatibility(short)}
     */
    public RdfaParserConfig(boolean verifyData, boolean stopAtFirstError,
                            boolean preserveBNodeIDs, RDFParser.DatatypeHandling dtHandling,
                            boolean enableProcessorGraph, boolean enableVocabExpansion, short rdfaCompatibility) {
        super(verifyData, stopAtFirstError, preserveBNodeIDs, dtHandling);
        this.processorGraphEnabled = enableProcessorGraph;
        this.vocabExpansionEnabled = enableVocabExpansion;
        this.rdfaCompatibility = rdfaCompatibility;
    }

    /**
     * @return {@link org.semarglproject.rdf.rdfa.RdfaParser#ENABLE_PROCESSOR_GRAPH} setting
     */
    public final boolean isProcessorGraphEnabled() {
        return processorGraphEnabled;
    }

    /**
     * @return {@link org.semarglproject.rdf.rdfa.RdfaParser#ENABLE_VOCAB_EXPANSION} setting
     */
    public final boolean isVocabExpansionEnabled() {
        return vocabExpansionEnabled;
    }

    /**
     * @return {@link org.semarglproject.rdf.rdfa.RdfaParser#RDFA_VERSION_PROPERTY} setting
     */
    public final short getRdfaCompatibility() {
        return rdfaCompatibility;
    }
}
