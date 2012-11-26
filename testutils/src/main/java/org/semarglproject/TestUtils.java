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

package org.semarglproject;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class TestUtils {

    private TestUtils() {
    }

    public static void dumpToStream(Document doc, OutputStream outputStream, boolean indent)
            throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        if (indent) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        StreamResult result = new StreamResult(outputStream);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
    }

    public static void dumpToStream(TripleCollection graph, OutputStream outputStream, String format) {
        Serializer serializer = Serializer.getInstance();
        serializer.serialize(outputStream, graph, format);
    }

    public static boolean areGraphsEqual(TripleCollection graph1, TripleCollection graph2) {
        if (graph1.size() != graph2.size()) {
            return false;
        }
        List<Triple> intersect = new ArrayList<Triple>();
        for (Triple triple1 : graph1) {
            int s = intersect.size();
            for (Triple triple2 : graph2) {
                if (intersect.contains(triple2)) {
                    continue;
                }
                if (isTriplesEqual(triple1, triple2)) {
                    intersect.add(triple2);
                    break;
                }
            }
            if (intersect.size() - s == 0) {
                return false;
            }
        }
        return intersect.size() == graph2.size();
    }

    private static boolean isTriplesEqual(Triple triple1, Triple triple2) {
        if (!triple1.getPredicate().equals(triple2.getPredicate())) {
            return false;
        }
        if (!isBothBNodesOrEqual(triple1.getObject(), triple2.getObject())) {
            return false;
        }
        return isBothBNodesOrEqual(triple1.getSubject(), triple2.getSubject());
    }

    private static boolean isBothBNodesOrEqual(Resource obj1, Resource obj2) {
        if (obj1 instanceof BNode) {
            if (!(obj2 instanceof BNode)) {
                return false;
            }
        } else if (!obj1.toString().replace("http://relative-uri.fake/", "")
                .equals(obj2.toString().replace("http://relative-uri.fake/", ""))) {
            return false;
        }
        return true;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        try {
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void downloadFile(String sourceUrl, File dest, boolean forced) throws IOException {
        if (!forced && dest.exists()) {
            return;
        }
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        URL url = new URL(sourceUrl);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(dest);
        FileChannel channel = fos.getChannel();
        try {
            channel.transferFrom(rbc, 0, 1 << 24);
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }

    public static void deleteDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteDir(child);
                }
            }
        }
        dir.delete();
    }

    public static String readFileToString(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    public static String readUrlToString(String url) throws IOException {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append('\n');
        }

        in.close();

        return response.toString();
    }

    public static void writeStringToFile(String str, File file) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            fc.write(ByteBuffer.wrap(str.getBytes(), 0, str.length()));
        } finally {
            stream.close();
        }
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

}