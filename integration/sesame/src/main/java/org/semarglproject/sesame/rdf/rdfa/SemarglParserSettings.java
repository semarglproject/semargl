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

import org.openrdf.rio.RioSetting;
import org.openrdf.rio.helpers.RioSettingImpl;
import org.semarglproject.rdf.rdfa.RdfaParser;

/**
 * Settings specific to Semargl that are not in {@link org.openrdf.rio.helpers.BasicParserSettings}
 * or {@link org.openrdf.rio.helpers.RDFaParserSettings} or {@link org.openrdf.rio.helpers.XMLParserSettings}.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 * @since 0.5
 */
public final class SemarglParserSettings {

    /**
     * Enables or disables generation of triples from processor graph.
     * ProcessorGraphHandler will receive events regardless of this option.
     * <p>
     * Defaults to false
     * @since 0.5
     */
    public static final RioSetting<Boolean> PROCESSOR_GRAPH_ENABLED = new RioSettingImpl<Boolean>(
            RdfaParser.ENABLE_PROCESSOR_GRAPH, "Processor Graph Enabled", Boolean.FALSE);

    private SemarglParserSettings() {
    }
}
