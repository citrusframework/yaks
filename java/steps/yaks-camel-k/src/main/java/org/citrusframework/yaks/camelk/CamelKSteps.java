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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static com.consol.citrus.container.Assert.Builder.assertException;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;

public class CamelKSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    private boolean autoRemoveResources = CamelKSettings.isAutoRemoveResources();
    private int maxAttempts = CamelKSettings.getMaxAttempts();
    private long delayBetweenAttempts = CamelKSettings.getDelayBetweenAttempts();

    private List<String> propertyFiles;

    private boolean supportVariablesInSources = CamelKSettings.isSupportVariablesInSources();

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        propertyFiles = new ArrayList<>();
    }

    @Given("^Disable auto removal of Camel-K resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

	@Given("^Enable auto removal of Camel-K resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;
    }

	@Given("^Disable variable support in Camel-K sources$")
    public void disableVariableSupport() {
        supportVariablesInSources = false;
    }

	@Given("^Enable variable support in Camel-K sources$")
    public void enableVariableSupport() {
        supportVariablesInSources = true;
    }

	@Given("^Camel-K resource polling configuration$")
    public void configureResourcePolling(Map<String, Object> configuration) {
        maxAttempts = Integer.parseInt(configuration.getOrDefault("maxAttempts", maxAttempts).toString());
        delayBetweenAttempts = Long.parseLong(configuration.getOrDefault("delayBetweenAttempts", delayBetweenAttempts).toString());
    }

	@Given("^Camel-K integration property file ([^\\s]+)$")
    public void addPropertyFile(String filePath) {
        propertyFiles.add(filePath);
    }

	@Given("^(?:create|new) Camel-K integration ([a-z0-9][a-z0-9-\\.]+[a-z0-9])\\.([a-z0-9-]+) with configuration:?$")
	public void createNewIntegration(String name, String language, Map<String, String> configuration) {
		if (configuration.get("source") == null) {
			throw new IllegalStateException("Specify 'source' parameter");
		}

		String integrationName = configuration.getOrDefault("name", name.toLowerCase());

		runner.run(camelk()
                    .client(k8sClient)
                    .createIntegration(integrationName + "." + language)
                    .source(configuration.get("source"))
                    .dependencies(configuration.getOrDefault("dependencies", "").trim())
                    .properties(configuration.getOrDefault("properties", "").trim())
                    .propertyFiles(propertyFiles)
                    .supportVariables(Boolean.parseBoolean(
                            configuration.getOrDefault("supportVariables", String.valueOf(supportVariablesInSources))))
                    .traits(configuration.get("traits")));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteIntegration(integrationName)));
        }
	}

	@Given("^load Camel-K integration ([a-zA-Z0-9][a-zA-Z0-9-\\.]+[a-zA-Z0-9])\\.([a-z0-9-]+)$")
	public void loadIntegrationFromFile(String name, String language) {
        Resource resource = new ClassPathResource(name + "." + language);
        try {
            createNewIntegration(name, language, FileUtils.readToString(resource));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load Camel-K integration from resource %s", name + "." + language));
        }
    }

    @Given("^load Camel-K integration ([a-zA-Z0-9][a-zA-Z0-9-\\.]+[a-zA-Z0-9])\\.([a-z0-9-]+) with configuration:?$")
	public void loadIntegrationFromFile(String name, String language, Map<String, String> configuration) {
        try {
            Resource resource = new ClassPathResource(name + "." + language);
            configuration.put("source", FileUtils.readToString(resource));
            createNewIntegration(name, language, configuration);
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load Camel-K integration from resource %s", name + "." + language));
        }
    }

    @Given("^(?:create|new) Camel-K integration ([a-z0-9][a-z0-9-\\.]+[a-z0-9])\\.([a-z0-9-]+)$")
	public void createNewIntegration(String name, String language, String source) {
        String integrationName = name.toLowerCase();

        runner.run(camelk()
                    .client(k8sClient)
                    .createIntegration(integrationName + "." + language)
                    .propertyFiles(propertyFiles)
                    .supportVariables(supportVariablesInSources)
                    .source(source));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteIntegration(integrationName)));
        }
	}

    @Given("^delete Camel-K integration ([a-z0-9-]+)$")
	public void deleteIntegration(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteIntegration(name));
	}

    @Given("^wait for Camel-K integration ([a-z0-9-]+)$")
    @Given("^Camel-K integration ([a-z0-9-]+) is running$")
    @Then("^Camel-K integration ([a-z0-9-]+) should be running$")
    public void integrationShouldBeRunning(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .isRunning());
    }

    @Given("^Camel-K integration ([a-z0-9-]+) is stopped")
    @Then("^Camel-K integration ([a-z0-9-]+) should be stopped")
    public void integrationShouldBeStopped(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .isStopped());
    }

    @Then("^Camel-K integration ([a-z0-9-]+) should print (.*)$")
    public void integrationShouldPrint(String name, String message) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .waitForLogMessage(message));
    }

    @Then("^Camel-K integration ([a-z0-9-]+) should not print (.*)$")
    public void integrationShouldNotPrint(String name, String message) {
        runner.run(assertException()
                .exception(ActionTimeoutException.class)
                .when(camelk()
                    .client(k8sClient)
                    .verifyIntegration(name)
                    .maxAttempts(maxAttempts)
                    .delayBetweenAttempts(delayBetweenAttempts)
                    .waitForLogMessage(message)));
    }
}
