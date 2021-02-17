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
import java.nio.file.Files;
import java.util.List;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.SummaryPrinter;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestRunFinished;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Deppisch
 */
public class ReportVerifyPlugin implements SummaryPrinter, EventListener {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ReportVerifyPlugin.class);

    @Override
    public void setEventPublisher(EventPublisher eventPublisher) {
        eventPublisher.registerHandlerFor(TestRunFinished.class, event -> {
            try {
                Assert.assertTrue("Verify termination log exists", Files.exists(TestReporter.getTerminationLog()));
                List<String> lines = Files.readAllLines(TestReporter.getTerminationLog());
                Assert.assertEquals(1L, lines.size());
                Assert.assertEquals("{" +
                            "\"suiteName\":\"Test reporter\"," +
                            "\"summary\":" +
                                "{\"passed\":1,\"failed\":0,\"errors\":0,\"skipped\":0,\"pending\":0,\"undefined\":0,\"total\":1}," +
                            "\"tests\":[" +
                                "{\"name\":\"Success test\",\"classname\":\"report.feature:3\"}" +
                            "]" +
                        "}", lines.get(0));
            } catch (IOException e) {
                LOG.warn("Failed to verify termination logs", e);
                Assert.fail(e.getMessage());
            }
        });
    }
}
