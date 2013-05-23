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
package org.semarglproject.sesame.rdf.rdfa;

import org.openrdf.rio.RioSetting;
import org.openrdf.rio.helpers.RDFaParserSettings;
import org.openrdf.rio.helpers.RioSettingImpl;
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
    public static final RioSetting<Boolean> PROCESSOR_GRAPH_ENABLED = new RioSettingImpl<Boolean>(
            RdfaParser.ENABLE_PROCESSOR_GRAPH, "Vocabulary Expansion", Boolean.FALSE);

    /**
     * TODO: Javadoc this setting
     * <p>
     * Defaults to null
     * @since 0.5
     */
    public static final RioSetting<XMLReader> CUSTOM_XML_READER = new RioSettingImpl<XMLReader>(
            StreamProcessor.XML_READER_PROPERTY, "Custom XML Reader", null);

    private SemarglParserSettings() {
    }
}
