/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.Citrus;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusFramework;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.ActionTimeoutException;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.spi.Resource;
import org.citrusframework.util.FileUtils;
import org.citrusframework.yaks.camelk.actions.integration.CreateIntegrationAction;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariable;
import static org.citrusframework.container.Assert.Builder.assertException;
import static org.citrusframework.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;

public class CamelKSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    private boolean autoRemoveResources = CamelKSettings.isAutoRemoveResources();
    private int maxAttempts = CamelKSettings.getMaxAttempts();
    private long delayBetweenAttempts = CamelKSettings.getDelayBetweenAttempts();

    private List<String> propertyFiles;
    private Map<String, String> properties;
    private List<String> buildPropertyFiles;
    private Map<String, String> buildProperties;

    private boolean supportVariablesInSources = CamelKSettings.isSupportVariablesInSources();

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        propertyFiles = new ArrayList<>();
        properties = new LinkedHashMap<>();
        buildPropertyFiles = new ArrayList<>();
        buildProperties = new LinkedHashMap<>();

        if (!context.getVariables().containsKey(VariableNames.OPERATOR_NAMESPACE.value())) {
            context.setVariable(VariableNames.OPERATOR_NAMESPACE.value(), CamelKSettings.getOperatorNamespace());
        }
    }

    @Given("^Disable auto removal of Camel K resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

	@Given("^Enable auto removal of Camel K resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;
    }

	@Given("^Disable variable support in Camel K sources$")
    public void disableVariableSupport() {
        supportVariablesInSources = false;
    }

	@Given("^Enable variable support in Camel K sources$")
    public void enableVariableSupport() {
        supportVariablesInSources = true;
    }

	@Given("^Camel K resource polling configuration$")
    public void configureResourcePolling(Map<String, Object> configuration) {
        maxAttempts = Integer.parseInt(configuration.getOrDefault("maxAttempts", maxAttempts).toString());
        delayBetweenAttempts = Long.parseLong(configuration.getOrDefault("delayBetweenAttempts", delayBetweenAttempts).toString());
    }

    @Given("^Camel K namespace ([^\\s]+)$")
    public void setNamespace(String namespace) {
        // update the test variable that points to the namespace
        runner.run(createVariable(VariableNames.CAMELK_NAMESPACE.value(), namespace));
    }

	@Given("^Camel K operator namespace ([^\\s]+)$")
    public void setOperatorNamespace(String namespace) {
        // update the test variable that points to the Camel K operator namespace
        runner.run(createVariable(VariableNames.OPERATOR_NAMESPACE.value(), namespace));
    }

	@Given("^Camel K integration property file ([^\\s]+)$")
    public void addPropertyFile(String filePath) {
        propertyFiles.add(filePath);
    }

    @Given("^Camel K integration property ([^\\s]+)=\"([^\"]*)\"$")
    @Given("^Camel K integration property ([^\\s]+) (?:is|=) \"([^\"]*)\"$")
    public void addProperty(String name, String value) {
        properties.put(name, value);
    }

	@Given("^Camel K integration properties$")
    public void addProperties(DataTable propertyTable) {
        properties.putAll(propertyTable.asMap(String.class, String.class));
    }

    @Given("^Camel K integration build property file ([^\\s]+)$")
    public void addBuildPropertyFile(String filePath) {
        buildPropertyFiles.add(filePath);
    }

	@Given("^Camel K integration build property ([^\\s]+)=\"([^\"]*)\"$")
    @Given("^Camel K integration build property ([^\\s]+) (?:is|=) \"([^\"]*)\"$")
    public void addBuildProperty(String name, String value) {
        buildProperties.put(name, value);
    }

	@Given("^Camel K integration build properties$")
    public void addBuildProperties(DataTable propertyTable) {
        buildProperties.putAll(propertyTable.asMap(String.class, String.class));
    }

	@Given("^(?:create|new) Camel K integration ([a-z0-9][a-z0-9-\\.]+[a-z0-9])\\.([a-z0-9-]+) with configuration:?$")
	public void createIntegration(String name, String language, Map<String, String> configuration) {
		if (configuration.get("source") == null) {
			throw new IllegalStateException("Specify 'source' parameter");
		}

		createIntegration(name, language, configuration.get("source"), configuration);
	}

	@Given("^load Camel K integration ([a-zA-Z0-9][a-zA-Z0-9-\\.]+[a-zA-Z0-9])\\.([a-z0-9-]+)$")
	public void loadIntegrationFromFile(String name, String language) {
        try {
            createIntegration(name, language, FileUtils.readToString(FileUtils.getFileResource(name + "." + language)));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load Camel K integration from resource %s", name + "." + language), e);
        }
    }

    @Given("^load Camel K integration ([a-zA-Z0-9][a-zA-Z0-9-\\.]+[a-zA-Z0-9])\\.([a-z0-9-]+) with configuration:?$")
	public void loadIntegrationFromFile(String name, String language, Map<String, String> configuration) {
        try {
            createIntegration(name, language, FileUtils.readToString(FileUtils.getFileResource(name + "." + language)), configuration);
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load Camel K integration from resource %s", name + "." + language), e);
        }
    }

    @Given("^(?:create|new) Camel K integration ([a-z0-9][a-z0-9-\\.]+[a-z0-9])\\.([a-z0-9-]+)$")
	public void createIntegration(String name, String language, String source) {
        runner.run(camelk()
                    .client(k8sClient)
                    .createIntegration(name)
                    .properties(properties)
                    .propertyFiles(propertyFiles)
                    .buildProperties(buildProperties)
                    .buildPropertyFiles(buildPropertyFiles)
                    .supportVariables(supportVariablesInSources)
                    .source(name + "." + language, source));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteIntegration(name)));
        }
	}

    @Given("^delete Camel K integration ([a-z0-9-]+)$")
	public void deleteIntegration(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteIntegration(name));
	}

    @Given("^wait for Camel K integration ([a-z0-9-]+)$")
    @Given("^Camel K integration ([a-z0-9-]+) is running$")
    @Then("^Camel K integration ([a-z0-9-]+) should be running$")
    public void integrationShouldBeRunning(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .isRunning());
    }

    @Given("^Camel K integration ([a-z0-9-]+) is stopped")
    @Then("^Camel K integration ([a-z0-9-]+) should be stopped")
    public void integrationShouldBeStopped(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .isStopped());
    }

    @Then("^Camel K integration ([a-z0-9-]+) should print (.*)$")
    public void integrationShouldPrint(String name, String message) {
        integrationShouldPrintMultiline(name, message);
    }

    @Then("^Camel K integration ([a-z0-9-]+) should print$")
    public void integrationShouldPrintMultiline(String name, String message) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyIntegration(name)
                .printLogs(CamelKSettings.isPrintPodLogs())
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .waitForLogMessage(message));
    }

    @Then("^Camel K integration ([a-z0-9-]+) should not print (.*)$")
    public void integrationShouldNotPrint(String name, String message) {
        integrationShouldNotPrintMultiline(name, message);
    }

    @Then("^Camel K integration ([a-z0-9-]+) should not print$")
    public void integrationShouldNotPrintMultiline(String name, String message) {
        runner.run(assertException()
                .exception(ActionTimeoutException.class)
                .when(camelk()
                    .client(k8sClient)
                    .verifyIntegration(name)
                    .printLogs(CamelKSettings.isPrintPodLogs())
                    .maxAttempts(maxAttempts)
                    .delayBetweenAttempts(delayBetweenAttempts)
                    .waitForLogMessage(message)));
    }

    private void createIntegration(String name, String language, String source, Map<String, String> configuration) {
        CreateIntegrationAction.Builder create = camelk()
                .client(k8sClient)
                .createIntegration(configuration.getOrDefault("name", name))
                .source(name + "." + language, source)
                .dependencies(configuration.getOrDefault("dependencies", "").trim())
                .buildProperties(configuration.getOrDefault("build-properties", "").trim())
                .buildProperties(buildProperties)
                .buildPropertyFiles(buildPropertyFiles)
                .properties(configuration.getOrDefault("properties", "").trim())
                .properties(properties)
                .propertyFiles(propertyFiles)
                .supportVariables(Boolean.parseBoolean(
                        configuration.getOrDefault("supportVariables", String.valueOf(supportVariablesInSources))))
                .traits(configuration.getOrDefault("traits", "").trim());

        String openApiSpec = configuration.getOrDefault("openapi", "");
        if (!openApiSpec.isEmpty()) {
            try {
                Resource file = FileUtils.getFileResource(openApiSpec);
                create.openApi(FileUtils.getFileName(file.getLocation()), FileUtils.readToString(file));
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to read openapi spec form file path %s", openApiSpec));
            }
        }

        runner.run(create);

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteIntegration(configuration.getOrDefault("name", name))));
        }
    }
}
