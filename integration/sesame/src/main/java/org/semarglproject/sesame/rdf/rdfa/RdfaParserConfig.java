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

public class RdfaParserConfig extends ParserConfig {

    private final boolean processorGraphEnabled;
    private final boolean vocabExpansionEnabled;
    private final short rdfaCompatibility;

    public RdfaParserConfig(boolean enableProcessorGraph, boolean enableVocabExpansion, short rdfaCompatibility) {
        super(false, true, true, RDFParser.DatatypeHandling.IGNORE);
        this.processorGraphEnabled = enableProcessorGraph;
        this.vocabExpansionEnabled = enableVocabExpansion;
        this.rdfaCompatibility = rdfaCompatibility;
    }

    public RdfaParserConfig(boolean verifyData, boolean stopAtFirstError,
                            boolean preserveBNodeIDs, RDFParser.DatatypeHandling datatypeHandling,
                            boolean enableProcessorGraph, boolean enableVocabExpansion, short rdfaCompatibility) {
        super(verifyData, stopAtFirstError, preserveBNodeIDs, datatypeHandling);
        this.processorGraphEnabled = enableProcessorGraph;
        this.vocabExpansionEnabled = enableVocabExpansion;
        this.rdfaCompatibility = rdfaCompatibility;
    }

    public final boolean isProcessorGraphEnabled() {
        return processorGraphEnabled;
    }

    public final boolean isVocabExpansionEnabled() {
        return vocabExpansionEnabled;
    }

    public final short getRdfaCompatibility() {
        return rdfaCompatibility;
    }
}
