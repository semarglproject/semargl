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

package org.semarglproject.rdf;

public interface TripleSink extends DataSink {

    void addNonLiteral(String subj, String pred, String obj);

    void addIriRef(String subj, String pred, String iri);

    void addPlainLiteral(String subj, String pred, String content, String lang);

    void addTypedLiteral(String subj, String pred, String content, String type);

    void startStream();

    void endStream();

}
