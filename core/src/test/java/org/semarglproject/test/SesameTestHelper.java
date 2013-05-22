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

package org.semarglproject.test;

import org.apache.commons.io.FileUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResultHandlerBase;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailTupleQuery;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.sail.memory.MemoryStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SesameTestHelper {

    private final String testOutputDir;
    private final Map<String, String> localMirrors;

    public SesameTestHelper(String testOutputDir, Map<String, String> localMirrors) {
        this.testOutputDir = testOutputDir;
        this.localMirrors = localMirrors;
        try {
            File testDir = new File(testOutputDir);
            testDir.mkdirs();
            FileUtils.cleanDirectory(testDir);
        } catch (IOException e) {
            // do nothing
        }
    }

    public static RDFFormat detectFileFormat(String filename) {
        if (filename.endsWith(".nt")) {
            return RDFFormat.NTRIPLES;
        } else if (filename.endsWith(".nq")) {
            return RDFFormat.NQUADS;
        }  else if (filename.endsWith(".ttl")) {
            return RDFFormat.TURTLE;
        } else if (filename.endsWith(".rdf")) {
            return RDFFormat.RDFXML;
        } else {
            throw new IllegalArgumentException("Unknown file format");
        }
    }

    public InputStream openStreamForResource(String uri)
            throws FileNotFoundException {
        String result = uri;
        for (String remoteUri : localMirrors.keySet()) {
            if (uri.startsWith(remoteUri)) {
                result = uri.replace(remoteUri, localMirrors.get(remoteUri));
            }
        }
        File file = new File(result);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        result = SesameTestHelper.class.getClassLoader().getResource(result).getFile();
        if (result.contains(".jar!/")) {
            try {
                return new URL("jar:" + result).openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return new FileInputStream(result);
    }

    public String getOutputPath(String uri, String ext) {
        String result = uri;
        for (String remoteUri : localMirrors.keySet()) {
            if (uri.startsWith(remoteUri)) {
                result = uri.replace(remoteUri, testOutputDir);
            }
        }
        result = result.substring(0, result.lastIndexOf('.')) + "-out." + ext;
        return result;
    }

    public Collection<Statement> createModelFromFile(String filename, String baseUri) throws IOException {
        if (filename == null) {
            return new ArrayList<Statement>();
        }
        final Collection<Statement> result = new ArrayList<Statement>();
        RDFParser rdfParser = Rio.createParser(SesameTestHelper.detectFileFormat(filename));
        rdfParser.setRDFHandler(new RDFHandlerBase() {
            @Override
            public void handleStatement(Statement statement) throws RDFHandlerException {
                result.add(statement);
            }
        });
        try {
            rdfParser.parse(openStreamForResource(filename), baseUri);
        } catch (OpenRDFException e) {
            e.printStackTrace();
        }
        return result;
    }

    public <E> List<E> getTestCases(final String manifestUri, String queryStr, final Class<E> template) {
        SailRepository repository =  new SailRepository(new MemoryStore());
        final List<E> testCases = new ArrayList<E>();
        try {
            repository.initialize();
            repository.getConnection().add(openStreamForResource(manifestUri),
                    manifestUri, SesameTestHelper.detectFileFormat(manifestUri));
            SailTupleQuery query = repository.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queryStr, manifestUri);
            query.evaluate(new TupleQueryResultHandlerBase() {
                @Override
                public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
                    try {
                        Object testCase = template.newInstance();
                        for (String fieldName : bindingSet.getBindingNames()) {
                            try {
                                template.getDeclaredField(fieldName).set(testCase,
                                        bindingSet.getBinding(fieldName).getValue().stringValue());
                            } catch (NoSuchFieldException e) {
                            }
                        }
                        testCases.add((E) testCase);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
            return testCases;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean areModelsEqual(String producedModelPath, String expectedModelPath, String baseUri) {
        try {
            Collection<Statement> inputModel = createModelFromFile(producedModelPath, baseUri);
            Collection<Statement> expected = createModelFromFile(expectedModelPath, baseUri);
            return ModelUtil.equals(inputModel, expected);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean askModel(String resultFilePath, String queryStr, String inputUri) {
        SailRepository repository =  new SailRepository(new MemoryStore());
        try {
            repository.initialize();
            repository.getConnection().add(openStreamForResource(resultFilePath),
                    inputUri, SesameTestHelper.detectFileFormat(resultFilePath));
            BooleanQuery query = repository.getConnection().prepareBooleanQuery(QueryLanguage.SPARQL, queryStr, inputUri);
            return query.evaluate();
        } catch (Exception e) {
            return false;
        }
    }
}
