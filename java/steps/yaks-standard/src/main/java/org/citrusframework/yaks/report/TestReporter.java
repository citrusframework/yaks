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

package org.citrusframework.yaks.report;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.StringJoiner;

import com.consol.citrus.CitrusInstanceManager;
import com.consol.citrus.TestResult;
import com.consol.citrus.cucumber.CitrusReporter;
import com.consol.citrus.report.AbstractTestReporter;
import com.consol.citrus.report.TestResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reporter writing test results summary to termination log. This information will be accessible via
 * pod container status details.
 *
 * @author Christoph Deppisch
 */
public class TestReporter extends CitrusReporter {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(TestReporter.class);

    private static final String TERMINATION_LOG_DEFAULT = "target/termination.log";
    private static final String TERMINATION_LOG_PROPERTY = "yaks.termination.log";
    private static final String TERMINATION_LOG_ENV = "YAKS_TERMINATION_LOG";

    static {
        TerminationLogReporter reporter = new TerminationLogReporter();

        CitrusInstanceManager.addInstanceProcessor(citrus -> {
            citrus.addTestReporter(reporter);
        });
    }

    static class TerminationLogReporter extends AbstractTestReporter {
        @Override
        public void generate(TestResults testResults) {
            StringJoiner report = new StringJoiner(System.lineSeparator());
            testResults.doWithResults(result -> report.add(getTestResultMessage(result)));

            try (Writer terminationLogWriter = Files.newBufferedWriter(getTerminationLog(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                terminationLogWriter.write(report.toString());
                terminationLogWriter.flush();
            } catch (IOException e) {
                LOG.warn(String.format("Failed to write termination logs to file '%s'", getTerminationLog()), e);
            }
        }

        private String getTestResultMessage(TestResult result) {
            if (result.isSuccess()) {
                return result.getTestName() + " SUCCESS";
            } else if (result.isSkipped()) {
                return result.getTestName() + " SKIPPED";
            } else if (result.isFailed()) {
                String errorType = result.getCause().getClass().getName();
                String errorMessage = Optional.ofNullable(result.getCause().getMessage()).orElse("Unknown error");

                return String.format("%s FAILED - Caused by: %s: %s%n",
                        result.getTestName(),
                        errorType,
                        errorMessage);
            }

            return result.getTestName() + " UNKNOWN STATE";
        }
    }

    public static Path getTerminationLog() {
        return Paths.get(System.getProperty(TERMINATION_LOG_PROPERTY,
                System.getenv(TERMINATION_LOG_ENV) != null ? System.getenv(TERMINATION_LOG_ENV) : TERMINATION_LOG_DEFAULT));
    }
}
