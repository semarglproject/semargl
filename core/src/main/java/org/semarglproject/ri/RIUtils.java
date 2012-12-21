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

package org.semarglproject.ri;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

// TODO: implement http://www.ietf.org/rfc/rfc2396.txt
// TODO: implement http://www.ietf.org/rfc/rfc2732.txt
// TODO: implement http://www.ietf.org/rfc/rfc3987.txt
public final class RIUtils {

    private RIUtils() {
    }

    private static final Pattern ABS_OPAQUE_IRI_PATTERN = Pattern.compile(
            "[a-zA-Z][a-zA-Z0-9+.-]*:" +     // scheme
            "[^#/][^#]*",                    // opaque part
            Pattern.DOTALL);

    private static final Pattern ABS_HIER_IRI_PATTERN = Pattern.compile(
            "[a-zA-Z][a-zA-Z0-9+.-]*:" +     // scheme
            "//?(([^/?#@]*)@)?" +            // user
            "(\\[[^@/?#]+\\]|([^@/?#:]+))" + // host
            "(:([^/?#]*))?" +                // port
            "([^#?]*)?" +                    // path
            "(\\?([^#]*))?" +                // query
            "(#[^#]*)?",                     // fragment
            Pattern.DOTALL);

    private static final Pattern URN_PATTERN = Pattern.compile("urn:[a-zA-Z0-9][a-zA-Z0-9-]{1,31}:.+");

    public static String resolveIri(String base, String iri) throws MalformedIriException {
        if (iri == null) {
            return null;
        }
        if (isIri(iri) || isUrn(iri)) {
            return iri;
        } else {
            if (iri.startsWith("?") || iri.isEmpty()) {
                if (base.endsWith("#")) {
                    return base.substring(0, base.length() - 1) + iri;
                }
                return base + iri;
            }
            String result;
            try {
                URL basePart = new URL(base);
                result = new URL(basePart, iri).toString();
            } catch (MalformedURLException e) {
                result = base + iri;
            }
            if (isIri(result)) {
                return result;
            }
            throw new MalformedIriException("Malformed IRI: " + iri);
        }
    }

    public static boolean isIri(String value) {
        return ABS_HIER_IRI_PATTERN.matcher(value).matches() || ABS_OPAQUE_IRI_PATTERN.matcher(value).matches();
    }

    public static boolean isAbsoluteIri(String value) {
        return ABS_HIER_IRI_PATTERN.matcher(value).matches();
    }

    public static boolean isUrn(String value) {
        return URN_PATTERN.matcher(value).matches();
    }

}
