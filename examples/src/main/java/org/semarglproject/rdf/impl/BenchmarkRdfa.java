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

package org.semarglproject.rdf.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import net.rootdev.javardfa.ParserFactory;
import net.rootdev.javardfa.jena.JenaStatementSink;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BenchmarkRdfa {

    private static final File BENCHMARK_PATH = new File("../rdfa/src/test/resources/rdfa-testsuite");
    public static final String HTTP_EXAMPLE_COM = "http://example.com";

    private static List<File> listFiles(File dir) {
        ArrayList<File> result = new ArrayList<File>();
        if (dir.exists()) {
            result.addAll(Arrays.asList(dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    // java-rdfa crashes here
                    if (s.contains("0314") || s.contains("0296") || s.contains("0233")
                            || s.contains("0175") || s.contains("0065")) {
                        return false;
                    }
                    return s.endsWith(".xhtml") || s.endsWith(".html");
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

    private static MGraph createClerezzaModel() {
        TcManager manager = TcManager.getInstance();
        UriRef graphUri = new UriRef(HTTP_EXAMPLE_COM);
        if (manager.listMGraphs().contains(graphUri)) {
            manager.deleteTripleCollection(graphUri);
        }
        return manager.createMGraph(graphUri);
    }

    private static long benchmarkSemarglJena(File path) throws FileNotFoundException, SAXException, ParseException {
        System.out.println("Semargl-Jena benchmark");
        Model model = ModelFactory.createDefaultModel();

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DataProcessor<Reader> dp = new SaxSource(xmlReader)
                .streamingTo(new RdfaParser()
                        .streamingTo(new JenaTripleSink(model))).build();

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            dp.process(new FileReader(file), HTTP_EXAMPLE_COM);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static long benchmarkJavaRdfaJena(File path) throws IOException, SAXException,
            ParseException, ClassNotFoundException {
        System.out.println("Java-Rdfa-Jena benchmark");
        Model model = ModelFactory.createDefaultModel();

        XMLReader reader = ParserFactory.createReaderForFormat(new JenaStatementSink(model), ParserFactory.Format.HTML);

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            try {
                reader.parse(new InputSource(new FileReader(file)));
            } catch (RuntimeException e) {
                // can't pass all tests
                System.err.println(file);
            }
        }
        System.out.println("Model size = " + model.size());
        model.write(new FileWriter("/home/lev/out.txt"), "TURTLE");
        return System.nanoTime() - time;
    }

    private static long benchmarkSemarglClerezza(File path) throws FileNotFoundException, SAXException, ParseException {
        System.out.println("Semargl-Clerezza benchmark");
        MGraph model = createClerezzaModel();

        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DataProcessor<Reader> dp = new SaxSource(xmlReader)
                .streamingTo(new RdfaParser()
                        .streamingTo(new ClerezzaTripleSink(model))).build();

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            dp.process(new FileReader(file), HTTP_EXAMPLE_COM);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static void printResults(long time) {
        System.out.println("Processing time: " + time / 1000000 + " ms");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        printResults(benchmarkSemarglJena(BENCHMARK_PATH));
        printResults(benchmarkSemarglClerezza(BENCHMARK_PATH));
        printResults(benchmarkJavaRdfaJena(BENCHMARK_PATH));
    }

}
