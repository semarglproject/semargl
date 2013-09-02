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
package org.semarglproject.example;

import org.openrdf.model.impl.AbstractModel;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.semarglproject.jsonld.JsonLdParser;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.sesame.core.sink.SesameSink;
import org.semarglproject.source.StreamProcessor;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BenchmarkJsonLd {

    private static final File BENCHMARK_PATH = new File("../jsonld/src/test/resources/json-ld-org");
    private static final String HTTP_EXAMPLE_COM = "http://example.com";

    private BenchmarkJsonLd() {
    }

    private static List<File> listFiles(File dir) {
        ArrayList<File> result = new ArrayList<File>();
        if (dir.exists()) {
            result.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    // not supported by semargl
                    if (s.contains("toRdf-0048") || s.contains("toRdf-0063") || s.contains("toRdf-0069") ||
                            s.contains("toRdf-0076") || s.contains("toRdf-0087") || s.contains("toRdf-0102") ||
                            s.contains("toRdf-0103") || s.contains("toRdf-0105")) {
                        return false;
                    }
                    // not supported by jsonld-java-sesame
                    if (s.contains("toRdf-0016") || s.contains("toRdf-0017") || s.contains("toRdf-0018") ||
                            s.contains("toRdf-0039") || s.contains("toRdf-0045") || s.contains("toRdf-0068") ||
                            s.contains("toRdf-0069") || s.contains("toRdf-0078") || s.contains("toRdf-0080") ||
                            s.contains("toRdf-0080") || s.contains("toRdf-0088") || s.contains("toRdf-0090") ||
                            s.contains("toRdf-0091") || s.contains("toRdf-0096") || s.contains("toRdf-0097") ||
                            s.contains("toRdf-0099") || s.contains("toRdf-0100") || s.contains("toRdf-0106")) {
                        return false;
                    }
                    return s.endsWith(".jsonld");
                }
            })));
            File[] childDirs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            for (File childDir : childDirs) {
                result.addAll(listFiles(childDir));
            }
        }
        return result;
    }

    private static long benchmarkSemarglSesame(File path) throws SAXException, ParseException {
        System.out.println("Semargl-Sesame benchmark");
        AbstractModel model = new LinkedHashModel();
        StreamProcessor streamProcessor = new StreamProcessor(JsonLdParser.connect(SesameSink.connect(new StatementCollector(model))));

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            streamProcessor.process(file, HTTP_EXAMPLE_COM);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static long benchmarkJsonLdJavaSesame(File path) throws SAXException, ParseException {
        System.out.println("JsonLd-Java-Sesame benchmark");
        AbstractModel model = new LinkedHashModel();

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            try {
                InputStream inputStream = new FileInputStream(file);
                String baseURI = "http://example.org/baseuri/";
                org.openrdf.model.Model statements = Rio.parse(inputStream, baseURI, RDFFormat.JSONLD);
                model.addAll(statements);
            } catch (Exception e) {
                System.out.println("Skipped " + file.getAbsolutePath() + " due to " + e.getMessage());
            }
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static void printResults(long time) {
        System.out.println("Processing time: " + time / 1000000 + " ms");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        printResults(benchmarkSemarglSesame(BENCHMARK_PATH));
        printResults(benchmarkJsonLdJavaSesame(BENCHMARK_PATH));
    }

}
