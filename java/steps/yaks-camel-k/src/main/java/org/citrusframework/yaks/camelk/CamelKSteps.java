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

import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.ActionTimeoutException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;

import static com.consol.citrus.container.Assert.Builder.assertException;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;

public class CamelKSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = CamelKSupport.getKubernetesClient(citrus);
        }
    }

	@Given("^(?:create|new) Camel-K integration ([a-z0-9-]+).([a-z0-9-]+) with configuration:$")
	public void createNewIntegration(String name, String language, Map<String, String> configuration) {
		if (configuration.get("source") == null) {
			throw new IllegalStateException("Specify 'source' parameter");
		}

		runner.run(camelk()
                    .client(k8sClient)
                    .createIntegration(name + "." + language)
                    .source(configuration.get("source"))
                    .dependencies(configuration.get("dependencies"))
                    .traits(configuration.get("traits")));

        if (CamelKSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteIntegration(name)));
        }
	}

	@Given("^(?:create|new) Camel-K integration ([a-z0-9-]+).([a-z0-9-]+)$")
	public void createNewIntegration(String name, String language, String source) {
        runner.run(camelk()
                    .client(k8sClient)
                    .createIntegration(name + "." + language)
                    .source(source));

        if (CamelKSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteIntegration(name)));
        }
	}

    @Given("^delete Camel-K integration ([a-z0-9-]+)$")
	public void deleteIntegration(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteIntegration(name));
	}

    @Given("^Camel-K integration ([a-z0-9-]+) is running$")
    @Then("^Camel-K integration ([a-z0-9-]+) should be running$")
    public void integrationShouldBeRunning(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .isRunning());
    }

    @Then("^Camel-K integration ([a-z0-9-]+) should print (.*)$")
    public void integrationShouldPrint(String name, String message) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .waitForLogMessage(message));
    }

    @Then("^Camel-K integration ([a-z0-9-]+) should not print (.*)$")
    public void integrationShouldNotPrint(String name, String message) {
        runner.run(assertException()
                .exception(ActionTimeoutException.class)
                .when(camelk()
                    .client(k8sClient)
                    .verifyIntegration(name)
                    .waitForLogMessage(message)));
    }

}
