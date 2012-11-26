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

package org.semarglproject.rdf.rdfa;

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
import java.util.HashSet;
import java.util.Set;

// http://github.com/rdfa/rdfa-website/raw/master/manifest.ttl
final class RdfaTestBundle {

    private final Collection<TestCase> testCases = new ArrayList<TestCase>();

    public final static class TestCase {
        private final String name;
        private final String input;
        private final String result;
        private final boolean expectedResult;

        public TestCase(String name, String input, String result, boolean expectedResult) {
            super();
            this.name = name;
            this.input = input;
            this.result = result;
            this.expectedResult = expectedResult;
        }

        public String getInput() {
            return input;
        }

        public String getResult() {
            return result;
        }

        public boolean getExpectedResult() {
            return expectedResult;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public RdfaTestBundle(String manifest, String manifestUri, String rdfaVersion, String docFormat) {
        super();

        String docExt = docFormat.replaceAll("[0-9]", "");

        String queryStr = null;
        Model graph = ModelFactory.createDefaultModel();
        try {
            graph.read(new FileInputStream(manifest), manifestUri, "TTL");
            queryStr = TestUtils.readFileToString(new File(
                    "src/test/resources/rdfa-testsuite/fetch_tests.sparql"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        queryStr = queryStr.replace("!rdfa_version", "\"" + rdfaVersion + "\"").replace(
                "!host_lang", "\"" + docFormat + "\"");

        Query query = QueryFactory.create(queryStr, manifestUri);
        QueryExecution qe = QueryExecutionFactory.create(query, graph);
        ResultSet rs = qe.execSelect();
        Set<String> addedCases = new HashSet<String>();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            String caseName = qs.getResource("test_case").getURI();

            // skip declaration duplicates in manifest
            if (addedCases.contains(caseName)) {
                continue;
            }
            addedCases.add(caseName);

            boolean expectedResult = true;
            if (qs.getLiteral("exp_result") != null) {
                expectedResult = qs.getLiteral("exp_result").getBoolean();
                // String descr = qs.getLiteral("descr").getString();
            }

            String pathDelta = rdfaVersion + "/" + docFormat + "/";
            String input = qs.getResource("input").getURI();
            input = fixTestFilePath(input, pathDelta, docExt);

            String result = qs.getResource("result").getURI();
            result = fixTestFilePath(result, pathDelta, "sparql");

            try {
                int testNum = Integer.parseInt(caseName.substring(caseName.lastIndexOf("/") + 1));
                // there is no way to detect rdfa version from that document
                if (testNum == 294 && rdfaVersion.equals("rdfa1.0") && docFormat.equals("svg")) {
                    continue;
                }
            } catch (NumberFormatException e) {
                // no problems here
            }

            testCases.add(new TestCase(caseName, input, result, expectedResult));
        }
        qe.close();
    }

    // There are some errors in manifest.ttl
    private static String fixTestFilePath(String input, String pathDelta, String actualExt) {
        int pos = input.lastIndexOf('/');
        String path = input.substring(0, pos + 1);
        String filename = input.substring(pos + 1);
        return path + pathDelta + filename.replaceAll("\\.\\w+$", "." + actualExt);
    }

    public Collection<TestCase> getTestCases() {
        return testCases;
    }
}
