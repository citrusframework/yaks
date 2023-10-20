/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.citrusframework.annotations.CitrusTestSource;
import org.citrusframework.common.TestLoader;
import org.citrusframework.junit.spring.JUnit4CitrusSpringSupport;
import org.citrusframework.report.TestReporter;
import org.citrusframework.report.TestResults;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.report.TestResult;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration(classes = Yaks_IT.Config.class)
public class Yaks_IT extends JUnit4CitrusSpringSupport {

    @Test
    @CitrusTestSource(type = TestLoader.YAML, packageScan = "org.citrusframework.yaks.yaml")
    public void yaksYaml_IT() {
    }

    @Configuration
    public static class Config {

        /** Logger */
        private static final Logger LOG = LoggerFactory.getLogger(Config.class);

        @Bean
        public TestReporter terminationLogReporter() {
            return testResults -> {
                try (Writer terminationLogWriter = Files.newBufferedWriter(YaksSettings.getTerminationLog(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    terminationLogWriter.write(convertResults(testResults).toJson());
                    terminationLogWriter.flush();
                } catch (IOException e) {
                    LOG.warn(String.format("Failed to write termination logs to file '%s'", YaksSettings.getTerminationLog()), e);
                }
            };
        }

        private static org.citrusframework.yaks.report.TestResults convertResults(TestResults testResults) {
            org.citrusframework.yaks.report.TestResults results = new org.citrusframework.yaks.report.TestResults();

            testResults.doWithResults(r ->
                    results.addTestResult(new TestResult(UUID.randomUUID(), r.getTestName(), r.getClassName(), r.getCause())));

            results.getSummary().passed = testResults.getSuccess();
            results.getSummary().failed = testResults.getFailed();
            results.getSummary().skipped = testResults.getSkipped();

            return results;
        }
    }
}
