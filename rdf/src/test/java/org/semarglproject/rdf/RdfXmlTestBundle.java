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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.semarglproject.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

final class RdfXmlTestBundle {

    public final static class TestCase {
        private final String name;
        private final String input;
        private final String result;

        public TestCase(String name, String input, String result) {
            super();
            this.name = name;
            this.input = input;
            this.result = result;
        }

        public String getInput() {
            return input;
        }

        public String getResult() {
            return result;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Collection<TestCase> testCases = new ArrayList<TestCase>();

    public RdfXmlTestBundle(String manifest, String manifestUri, String testsuiteRoot) {
        super();

        String queryStr = null;
        Model graph = ModelFactory.createDefaultModel();
        try {
            graph.read(new FileInputStream(manifest), manifestUri, "RDF/XML");
            queryStr = TestUtils.readFileToString(new File(
                    "src/test/resources/fetch_rdfxml_tests.sparql"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Query query = QueryFactory.create(queryStr, manifestUri);
        QueryExecution qe = QueryExecutionFactory.create(query, graph);
        ResultSet rs = qe.execSelect();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String testName = qs.getResource("test_case").getURI().replace(testsuiteRoot, "");
            String input = qs.getResource("input").getURI();

            String result = null;
            if (qs.getResource("result") != null) {
                result = qs.getResource("result").getURI();
            }

            // streaming parser shouldn't handle this
            if (input.endsWith("rdfms-difference-between-ID-and-about/error1.rdf")
                    || input.endsWith("rdfms-empty-property-elements/test013.rdf")
                    || input.endsWith("rdfms-not-id-and-resource-attr/test002.rdf")
                    || input.endsWith("rdfms-not-id-and-resource-attr/test005.rdf")

                    // Treatment of namespaces that are not
                    // visibly used is implementation dependent
                    || input.endsWith("rdfms-xml-literal-namespaces/test002.rdf")
                    || input.endsWith("xml-literals/html.rdf")
                    || input.endsWith("xml-literals/reported1.rdf")
                    || input.endsWith("xml-literals/reported2.rdf")
                    || input.endsWith("xml-literals/reported3.rdf")

                    // Jena fails here, but results are right
                    || input.endsWith("i18n/t9000.rdf")) {
                continue;
            }
            testCases.add(new TestCase(testName, input, result));
        }
        qe.close();
    }

    public Collection<TestCase> getTestCases() {
        return testCases;
    }
}
