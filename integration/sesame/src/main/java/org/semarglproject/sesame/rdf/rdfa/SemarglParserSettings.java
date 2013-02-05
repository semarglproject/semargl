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

import org.openrdf.rio.ParserSetting;
import org.openrdf.rio.helpers.ParserSettingImpl;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.vocab.RDFa;
import org.xml.sax.XMLReader;

/**
 * Settings specific to Semargl that are not in {@link org.openrdf.rio.helpers.BasicParserSettings}.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 * @since 0.5
 */
public final class SemarglParserSettings {

    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to false
     * @since 0.5
     */
    public static final ParserSetting<Boolean> VOCAB_EXPANSION_ENABLED = new ParserSettingImpl<Boolean>(
            RdfaParser.ENABLE_VOCAB_EXPANSION, "Vocabulary Expansion", Boolean.FALSE);

    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to false
     * @since 0.5
     */
    public static final ParserSetting<Boolean> PROCESSOR_GRAPH_ENABLED = new ParserSettingImpl<Boolean>(
            RdfaParser.ENABLE_PROCESSOR_GRAPH, "Vocabulary Expansion", Boolean.FALSE);

    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to 1.1
     * @since 0.5
     */
    public static final ParserSetting<Short> RDFA_COMPATIBILITY = new ParserSettingImpl<Short>(
            RdfaParser.RDFA_VERSION_PROPERTY, "RDFa Version Compatibility", RDFa.VERSION_11);

    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to null
     * @since 0.5
     */
    public static final ParserSetting<XMLReader> CUSTOM_XML_READER = new ParserSettingImpl<XMLReader>(
            StreamProcessor.XML_READER_PROPERTY, "Custom XML Reader", null);

    private SemarglParserSettings() {
    }
}
