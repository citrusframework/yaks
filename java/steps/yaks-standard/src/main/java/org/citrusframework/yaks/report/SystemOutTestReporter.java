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

import com.consol.citrus.report.AbstractTestSuiteListener;
import com.consol.citrus.report.LoggingReporter;
import com.consol.citrus.report.OutputStreamReporter;
import com.consol.citrus.report.TestReporter;
import com.consol.citrus.report.TestResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reporter activates when logging over logging framework is disabled in order to print
 * a minimal test report using System.out
 *
 * @author Christoph Deppisch
 */
public class SystemOutTestReporter extends AbstractTestSuiteListener implements TestReporter {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(SystemOutTestReporter.class);

    private final OutputStreamReporter delegate;

    public SystemOutTestReporter() {
        this.delegate = new OutputStreamReporter(new BufferedWriter(new OutputStreamWriter(System.out)));
        this.delegate.setFormat("%s | %s%n");
    }

    @Override
    public void onStart() {
        if (!LoggerFactory.getLogger(LoggingReporter.class).isInfoEnabled()) {
            try {
                delegate.onStart();
                delegate.getLogWriter().flush();
            } catch (IOException e) {
                LOG.warn("Failed to initialize test report", e);
            }
        }
    }

    @Override
    public void generateReport(TestResults testResults) {
        if (!LoggerFactory.getLogger(LoggingReporter.class).isInfoEnabled()) {
            try {
                delegate.generateReport(testResults);
                delegate.getLogWriter().flush();
            } catch (IOException e) {
                LOG.warn("Failed to write test summary report", e);
            }
        }
    }

    public void destroy() throws Exception {
        delegate.getLogWriter().close();
    }
}
