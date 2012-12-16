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

package org.semarglproject.vocab;

public final class XSD {
    public static final String NS = "http://www.w3.org/2001/XMLSchema#";

    public static final String DATE = NS + "date";
    public static final String TIME = NS + "time";
    public static final String DATE_TIME = NS + "dateTime";
    public static final String DURATION = NS + "duration";
    public static final String G_YEAR = NS + "gYear";
    public static final String G_YEAR_MONTH = NS + "gYearMonth";

    private XSD() {
    }
}
