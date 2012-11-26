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

// TODO: implement <http://www.ietf.org/rfc/rfc3987.txt>
public final class IRI {

    private IRI() {
    }

    private static final Pattern ABS_IRI_PATTERN = Pattern.compile("[^:/?#^]+://" + // scheme
            "(([^/?#@]*)@)?" + // user
            "(\\[[^@/?#]*\\]|([^@/?#:]*))" + // host
            "(:([^/?#]*))?" + // port
            "([^#?]*)?" + // path
            "(\\?([^#]*))?" + // query
            "(#(.*))?", // frag
            Pattern.DOTALL);
    private static final Pattern URN_PATTERN = Pattern
            .compile("urn:[a-zA-Z0-9][a-zA-Z0-9-]{1,31}:.+");

    public static String resolve(String base, String iri) throws MalformedIRIException {
        if (iri == null) {
            return null;
        }
        if (isAbsolute(iri) || isURN(iri)) {
            return iri;
        } else {
            if (base.endsWith("#")) {
                base = base.substring(0, base.length() - 1);
            }
            if (iri.startsWith("?")) {
                return base + iri;
            }
            if (iri.isEmpty()) {
                return base;
            }
            try {
                URL basePart = new URL(base);
                return new URL(basePart, iri).toString();
            } catch (MalformedURLException e) {
                String result = base + iri;
                if (isAbsolute(result)) {
                    return result;
                }
                throw new MalformedIRIException("Can not parse IRI");
            }
        }
    }

    public static boolean isAbsolute(String value) {
        return ABS_IRI_PATTERN.matcher(value).matches();
    }

    private static boolean isURN(String value) {
        return URN_PATTERN.matcher(value).matches();
    }

}
