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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import com.consol.citrus.cucumber.CitrusReporter;
import com.consol.citrus.report.LoggingReporter;
import com.consol.citrus.report.OutputStreamReporter;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestStepFinished;
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

    private TestResults testResults = new TestResults();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::initializeReports);
        publisher.registerHandlerFor(TestCaseFinished.class, this::saveTestResult);
        publisher.registerHandlerFor(TestCaseStarted.class, this::addTestDetail);
        publisher.registerHandlerFor(TestStepFinished.class, this::checkStepErrors);
        publisher.registerHandlerFor(TestRunFinished.class, this::printReports);
        super.setEventPublisher(publisher);
    }

    private void addTestDetail(TestCaseStarted event) {
        testResults.addTestResult(new TestResult(event.getTestCase().getId(), event.getTestCase().getName(),
                event.getTestCase().getUri() + ":" + event.getTestCase().getLine()));
    }

    /**
     * Adds step error to test results.
     * @param event
     */
    private void checkStepErrors(TestStepFinished event) {
        if (event.getResult().getError() != null
                && !(event.getTestStep() instanceof HookTestStep)) {
            Optional<TestResult> testDetail = testResults.getTests().stream()
                    .filter(detail -> detail.getId().equals(event.getTestCase().getId()))
                    .findFirst();

            if (testDetail.isPresent()) {
                testDetail.get().setCause(event.getResult().getError());
            } else {
                testResults.addTestResult(new TestResult(event.getTestCase().getId(), event.getTestCase().getName(),
                                            event.getTestCase().getUri() + ":" + event.getTestCase().getLine(), event.getResult().getError()));
            }
        }
    }

    private void initializeReports(TestRunStarted event) {
        if (!LoggerFactory.getLogger(LoggingReporter.class).isInfoEnabled()) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
                new OutputStreamReporter(writer).onStart();
                writer.flush();
            } catch (IOException e) {
                LOG.warn("Failed to initialize test report", e);
            }
        }
    }

    /**
     * Prints test results to termination log.
     * @param event
     */
    private void printReports(TestRunFinished event) {
        if (!LoggerFactory.getLogger(LoggingReporter.class).isInfoEnabled()) {
            com.consol.citrus.report.TestResults results = new com.consol.citrus.report.TestResults();
            testResults.getTests().forEach(r -> {
                if (r.getCause() != null) {
                    results.addResult(com.consol.citrus.TestResult.failed(r.getName(), r.getClassname(), r.getCause()));
                } else {
                    results.addResult(com.consol.citrus.TestResult.success(r.getName(), r.getClassname()));
                }
            });

            try (Writer writer = new BufferedWriter(new OutputStreamWriter(System.out))) {
                new OutputStreamReporter(writer).generateReport(results);
                writer.flush();
            } catch (IOException e) {
                LOG.warn("Failed to write test summary report", e);
            }
        }

        try (Writer terminationLogWriter = Files.newBufferedWriter(getTerminationLog(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            terminationLogWriter.write(testResults.toJson());
            terminationLogWriter.flush();
        } catch (IOException e) {
            LOG.warn(String.format("Failed to write termination logs to file '%s'", getTerminationLog()), e);
        }
    }

    /**
     * Save test result for later reporting.
     * @param event
     */
    private void saveTestResult(TestCaseFinished event) {
        switch (event.getResult().getStatus()) {
            case FAILED:
                testResults.getSummary().failed++;
                break;
            case PASSED:
                testResults.getSummary().passed++;
                break;
            case PENDING:
                testResults.getSummary().pending++;
                break;
            case UNDEFINED:
                testResults.getSummary().undefined++;
                break;
            case SKIPPED:
                testResults.getSummary().skipped++;
                break;
            default:
        }
    }

    public static Path getTerminationLog() {
        return Paths.get(System.getProperty(TERMINATION_LOG_PROPERTY,
                System.getenv(TERMINATION_LOG_ENV) != null ? System.getenv(TERMINATION_LOG_ENV) : TERMINATION_LOG_DEFAULT));
    }
}
