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

package org.citrusframework.yaks.camelk;

import java.io.IOException;
import java.util.Map;

import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.ActionTimeoutException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import static com.consol.citrus.container.Assert.Builder.assertException;
import static org.citrusframework.yaks.camelk.actions.CreateIntegrationTestAction.Builder.createIntegration;
import static org.citrusframework.yaks.camelk.actions.VerifyIntegrationTestAction.Builder.verifyIntegration;


public class CamelKSteps {

    private KubernetesClient client;

    @CitrusResource
    private TestCaseRunner runner;

	@Given("^new integration with name ([a-z0-9_]+\\.[a-z0-9_]+) with configuration:$")
	public void createNewIntegration(String name, Map<String, String> configuration) throws IOException {
		if(configuration.get("source") == null) {
			throw new IllegalStateException("Specify 'source' parameter");
		}

		runner.run(createIntegration()
                    .client(client())
                    .integrationName(name)
                    .source(configuration.get("source"))
                    .dependencies(configuration.get("dependencies"))
                    .traits(configuration.get("traits")));
	}

	@Given("^new integration with name ([a-z0-9_]+\\.[a-z0-9_]+)$")
	public void createNewIntegration(String name, String source) throws IOException {
        runner.run(createIntegration()
                    .client(client())
                    .integrationName(name)
                    .source(source));
	}

    @Given("^integration ([a-z0-9-.]+) is running$")
    @Then("^integration ([a-z0-9-.]+) should be running$")
    public void shouldBeRunning(String name) throws InterruptedException {
        runner.run(verifyIntegration()
                .isRunning(name));
    }

    @Then("^integration ([a-z0-9-.]+) should print (.*)$")
    public void shouldPrint(String name, String message) throws InterruptedException {
        runner.run(verifyIntegration()
                .client(client())
                .isRunning(name)
                .waitForLogMessage(message));
    }

    @Then("^integration ([a-z0-9-.]+) should not print (.*)$")
    public void shouldNotPrint(String name, String message) throws InterruptedException {
        runner.run(assertException()
                .exception(ActionTimeoutException.class)
                .when(verifyIntegration()
                    .client(client())
                    .isRunning(name)
                    .waitForLogMessage(message)));
    }

    /**
     * Lazy initialize a default Kubernetes client.
     * @return
     */
    private KubernetesClient client() {
        if (this.client == null) {
            this.client = new DefaultKubernetesClient();
        }
        return this.client;
    }

}
