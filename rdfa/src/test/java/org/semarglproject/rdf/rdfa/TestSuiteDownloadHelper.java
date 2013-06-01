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

package org.semarglproject.rdf.rdfa;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestSuiteDownloadHelper {

    public static void downloadAll(int parallelism) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        executorService.execute(new TestDownloadWorker("rdfa1.0", "xhtml1"));
        executorService.execute(new TestDownloadWorker("rdfa1.0", "svg"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "html4"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "xhtml1"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "html5"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "xml"));
        executorService.execute(new TestDownloadWorker("rdfa1.1", "svg"));
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);
    }

    private static void downloadIfMissing(String testUrl) throws IOException {
        File inputFile = new File(testUrl.replace(RdfaTestSuiteHelper.RDFA_TESTSUITE_ROOT,
                "src/test/resources/rdfa-testsuite/"));
        if (!inputFile.exists()) {
            FileUtils.copyURLToFile(new URL(testUrl), inputFile);
        }
    }

    private static class TestDownloadWorker implements Runnable {

        private Collection<RdfaTestSuiteHelper.TestCase> tests;

        private TestDownloadWorker(String rdfaVersion, String docFormat) {
            this.tests = RdfaTestSuiteHelper.getTestSuite(rdfaVersion, docFormat);
        }

        @Override
        public void run() {
            for (RdfaTestSuiteHelper.TestCase test : tests) {
                try {
                    downloadIfMissing(test.input);
                    downloadIfMissing(test.result);
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

}
