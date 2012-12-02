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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.semarglproject.rdf.DataProcessor;
import org.semarglproject.rdf.ParseException;
import org.semarglproject.rdf.SaxSource;
import org.semarglproject.rdf.TurtleStreamSink;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class RdfaProcessorWebService extends AbstractHandler {

    DataProcessor<Reader> dp;
    TurtleStreamSink ts = new TurtleStreamSink();
    RdfaParser rdfaParser = new RdfaParser().streamingTo(ts);

    {
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dp = new SaxSource(reader).streamingTo(rdfaParser).build();
        } catch (SAXException e) {
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new RdfaProcessorWebService());
        server.start();
        server.join();
    }

    @Override
    public void handle(String s, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String uri = request.getParameter("uri");
        if (uri == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            baseRequest.setHandled(false);
            return;
        }

        // parameter name specified by RDFa Core 1.1 par. 7.6.1
        String rdfaGraph = request.getParameter("rdfagraph");
        boolean sinkOutputGraph = false;
        boolean sinkProcessorGraph = false;
        if (rdfaGraph != null) {
            String[] keys = rdfaGraph.trim().toLowerCase().split(",");
            for (String key : keys) {
                if (key.equals("output")) {
                    sinkOutputGraph = true;
                } else if (key.equals("processor")) {
                    sinkProcessorGraph = true;
                }
            }
        } else {
            sinkOutputGraph = true;
            sinkProcessorGraph = true;
        }
        rdfaParser.setOutput(sinkOutputGraph, sinkProcessorGraph);

        String rdfaversion = request.getParameter("rdfaversion");
        if ("1.0".equals(rdfaversion)) {
            rdfaParser.setRdfaVersion(RdfaParser.RDFA_10);
        } else if ("1.1".equals(rdfaversion)) {
            rdfaParser.setRdfaVersion(RdfaParser.RDFA_11);
        }

        System.out.println(uri);
        URL url = new URL(uri);
        Reader reader = new InputStreamReader(url.openStream());

        ts.setStream(response.getOutputStream());
        response.setContentType("text/turtle;charset=utf-8");
        try {
            dp.process(reader, uri);
        } catch (ParseException e) {
            e.printStackTrace(System.out);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            baseRequest.setHandled(false);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }
}
