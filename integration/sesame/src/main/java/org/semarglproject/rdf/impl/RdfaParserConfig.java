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

package org.semarglproject.rdf.impl;

import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFParser;

public class RdfaParserConfig extends ParserConfig {

    private final boolean processorGraphEnabled;
    private final boolean vocabExpansionEnabled;
    private final short rdfaCompatibility;

    public RdfaParserConfig(boolean preserveBNodeIDs, boolean processorGraphEnabled,
                            boolean vocabExpansionEnabled, short rdfaCompatibility) {
        super(false, true, preserveBNodeIDs, RDFParser.DatatypeHandling.IGNORE);
        this.processorGraphEnabled = processorGraphEnabled;
        this.vocabExpansionEnabled = vocabExpansionEnabled;
        this.rdfaCompatibility = rdfaCompatibility;
    }

    public boolean isProcessorGraphEnabled() {
        return processorGraphEnabled;
    }

    public boolean isVocabExpansionEnabled() {
        return vocabExpansionEnabled;
    }

    public short getRdfaCompatibility() {
        return rdfaCompatibility;
    }
}
