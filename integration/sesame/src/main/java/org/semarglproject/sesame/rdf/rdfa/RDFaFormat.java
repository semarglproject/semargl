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

import org.openrdf.rio.RDFFormat;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public final class RDFaFormat {

    public static final RDFFormat RDFA = new RDFFormat("RDFa", Arrays.asList(
            "application/xhtml+xml", "text/html", "image/svg+xml"),
            Charset.forName("UTF-8"), Arrays.asList("xhtml, html, svg"), true, false);

    private RDFaFormat() {
    }

}
