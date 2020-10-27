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

package org.citrusframework.yaks.hooks;

import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import org.citrusframework.yaks.util.CucumberUtils;

/**
 * Failure hook makes sure that the Citrus test state is set to FAILED when the Cucumber scenario is failed. This is because
 * the error might not be within the Citrus test actions but in some general Cucumber step (e.g. when connection to JMS message broker failed).
 *
 * This would then lead to a false positive Citrus test report because the error was not part of the Citrus test. This hook makes sure to always set
 * an exception on the test context in order to correctly reflect the scenario failed state.
 *
 * @author Christoph Deppisch
 */
public class TestFailureHook {

    @CitrusResource
    private TestCaseRunner runner;

    @After(order = Integer.MAX_VALUE)
    public void checkTestFailure(Scenario scenario) {
        if (scenario.isFailed()) {
            runner.run(new AddErrorAction(String.format("Scenario '%s' in %s status %s",
                    scenario.getName(), CucumberUtils.extractFeatureFileName(scenario), scenario.getStatus().toString())));
        } else if (Status.PENDING == scenario.getStatus() || Status.UNDEFINED == scenario.getStatus()) {
            runner.run(new AddErrorAction(String.format("Scenario '%s' in %s has pending or undefined step(s)",
                    scenario.getName(), CucumberUtils.extractFeatureFileName(scenario))));
        }
    }

    /**
     * Test action adds new error to the test context.
     */
    private static class AddErrorAction extends AbstractTestAction {
        private final String errorMessage;

        public AddErrorAction(String msg) {
            this.errorMessage = msg;
        }

        @Override
        public void doExecute(TestContext context) {
            context.getExceptions().add(new CitrusRuntimeException(errorMessage));
        }
    }
}
