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

import java.util.HashMap;
import java.util.Map;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.AnyTypeBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.camel.v1.KameletBuilder;
import org.apache.camel.v1.KameletSpecBuilder;
import org.apache.camel.v1.kameletspec.DataTypes;
import org.apache.camel.v1.kameletspec.DefinitionBuilder;
import org.apache.camel.v1.kameletspec.SourcesBuilder;
import org.apache.camel.v1.kameletspec.datatypes.TypesBuilder;
import org.apache.camel.v1.kameletspec.definition.Properties;
import org.apache.camel.v1.kameletspec.definition.PropertiesBuilder;
import org.citrusframework.Citrus;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusFramework;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.spi.Resource;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.citrusframework.yaks.util.ResourceUtils;
import org.springframework.util.StringUtils;

import static org.citrusframework.actions.CreateVariablesAction.Builder.createVariable;
import static org.citrusframework.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;

public class KameletSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestContext context;

    private KubernetesClient k8sClient;

    private String kameletApiVersion = KameletSettings.getKameletApiVersion();

    // Kamelet builder
    private KameletBuilder kamelet;
    private KameletSpecBuilder kameletSpecBuilder;
    private Map<String, DataTypes> dataTypes;
    private DefinitionBuilder definition;
    private String kameletTemplate;

    private String namespace = KameletSettings.getNamespace();

    private boolean autoRemoveResources = CamelKSettings.isAutoRemoveResources();
    private boolean supportVariablesInSources = CamelKSettings.isSupportVariablesInSources();

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        initializeKameletBuilder();
    }

    @Given("^Disable auto removal of Kamelet resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;

        // update the test variable
        runner.run(createVariable(VariableNames.AUTO_REMOVE_RESOURCES.value(), "false"));
    }

    @Given("^Enable auto removal of Kamelet resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;

        // update the test variable
        runner.run(createVariable(VariableNames.AUTO_REMOVE_RESOURCES.value(), "true"));
    }

    @Given("^Disable variable support in Kamelet sources$")
    public void disableVariableSupport() {
        supportVariablesInSources = false;
    }

    @Given("^Enable variable support in Kamelet sources$")
    public void enableVariableSupport() {
        supportVariablesInSources = true;
    }

    @Given("^Kamelet API version (v1|v1alpha1)$")
    public void setKameletApiVersion(String apiVersion) {
        this.kameletApiVersion = apiVersion;

        // update the test variable that points to the api version
        runner.run(createVariable(VariableNames.KAMELET_API_VERSION.value(), apiVersion));
    }

    @Given("^Kamelet namespace ([^\\s]+)$")
    public void setNamespace(String namespace) {
        this.namespace = namespace;

        // update the test variable that points to the namespace
        runner.run(createVariable(VariableNames.KAMELET_NAMESPACE.value(), namespace));
    }

    @Given("^Kamelet dataType (in|out|error)(?:=| is )\"(.+)\"$")
	public void addType(String slot, String format) {
        DataTypes dt;
        if (dataTypes.containsKey(slot)) {
            dt = dataTypes.get(slot);
        } else {
            dt = new DataTypes();
            dataTypes.put(slot, dt);
        }

        if (dt.getTypes() == null) {
            dt.setTypes(new HashMap<>());
        }

        if (format.contains(":")) {
            String[] schemeAndFormat = format.split(":");
            dt.getTypes().put(schemeAndFormat[1], new TypesBuilder().withScheme(schemeAndFormat[0]).withFormat(schemeAndFormat[1]).build());
        } else {
            dt.getTypes().put(format, new TypesBuilder().withScheme("camel").withFormat(format).build());
        }
	}

    @Given("^Kamelet title \"(.+)\"$")
	public void setTitle(String title) {
        definition.withTitle(title);
	}

    @Given("^Kamelet source ([a-z0-9-]+).([a-z0-9-]+)$")
	public void setSource(String name, String language, String content) {
        kameletSpecBuilder.withSources(new SourcesBuilder().withName(name).withLanguage(language).withContent(content).build());
	}

    @Given("^Kamelet template")
	public void setFlow(String template) {
        kameletTemplate = template;
	}

    @Given("^Kamelet property definition$")
	public void addPropertyDefinition(Map<String, Object> propertyConfiguration) {
        if (!propertyConfiguration.containsKey("name")) {
            throw new CitrusRuntimeException("Missing property name in configuration. " +
                    "Please add the property name to the property definition");
        }

        addPropertyDefinition(propertyConfiguration.get("name").toString(), propertyConfiguration);
    }
    @Given("^Kamelet property definition ([^\\s]+)$")
	public void addPropertyDefinition(String propertyName, Map<String, Object> propertyConfiguration) {
        String type = propertyConfiguration.getOrDefault("type", "string").toString();
        String title = propertyConfiguration.getOrDefault("title", StringUtils.capitalize(propertyName)).toString();
        Object defaultValue = propertyConfiguration.get("default");
        Object example = propertyConfiguration.get("example");
        String required = propertyConfiguration.getOrDefault("required", Boolean.FALSE).toString();

        if (Boolean.parseBoolean(required)) {
            definition.addToRequired(propertyName);
        }

        Properties property = new PropertiesBuilder().withTitle(title).withType(type).build();
        if (example != null) {
            property.setExample(new AnyTypeBuilder().withValue(example).build());
        }

        if (defaultValue != null) {
            property.set_default(new AnyTypeBuilder().withValue(defaultValue).build());
        }
        definition.addToProperties(propertyName, property);
	}

    @Given("^load Kamelet ([a-z0-9-]+).kamelet.yaml$")
    public void loadKameletFromFile(String fileName) {
        Resource resource = ResourceUtils.resolve(fileName + ".kamelet.yaml", context);
        runner.run(camelk()
                .client(k8sClient)
                .createKamelet(fileName)
                .namespace(namespace)
                .apiVersion(kameletApiVersion)
                .supportVariables(supportVariablesInSources)
                .resource(resource));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deleteKamelet(fileName)
                                     .apiVersion(kameletApiVersion)));
        }
    }

    @Given("^(?:create|new) Kamelet ([a-z0-9-]+)$")
	public void createNewKamelet(String name) {
        kamelet.withNewMetadata()
                .withName(name)
            .endMetadata();

        if (definition.getTitle() == null || definition.getTitle().isEmpty()) {
            definition.withTitle(StringUtils.capitalize(name));
        }

        kameletSpecBuilder.withDefinition(definition.build());
        kameletSpecBuilder.withDataTypes(dataTypes);

        kamelet.withSpec(kameletSpecBuilder.build());

        runner.run(camelk()
                    .client(k8sClient)
                    .createKamelet(name)
                    .supportVariables(supportVariablesInSources)
                    .template(kameletTemplate)
                    .fromBuilder(kamelet));

        initializeKameletBuilder();

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deleteKamelet(name)
                                     .apiVersion(kameletApiVersion)));
        }
	}

    @Deprecated
	@Given("^(?:create|new) Kamelet ([a-z0-9-]+) with flow")
	public void createNewKameletWithFlow(String name, String flow) {
        createNewKameletWithTemplate(name, flow);
	}

    @Given("^(?:create|new) Kamelet ([a-z0-9-]+) with template")
	public void createNewKameletWithTemplate(String name, String template) {
        kameletTemplate = template;
        createNewKamelet(name);
	}

    @Given("^delete Kamelet ([a-z0-9-]+)$")
	public void deleteKamelet(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteKamelet(name)
                    .apiVersion(kameletApiVersion));
	}

    @Given("^Kamelet ([a-z0-9-]+) is available$")
    @Then("^Kamelet ([a-z0-9-]+) should be available$")
    public void kameletShouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyKamelet(name)
                .apiVersion(kameletApiVersion)
                .isAvailable());
    }

    @Given("^Kamelet ([a-z0-9-]+) is available in namespace ([a-z0-9-]+)$")
    @Then("^Kamelet ([a-z0-9-]+) should be available in namespace ([a-z0-9-]+)$")
    public void kameletShouldBeAvailable(String name, String namespace) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyKamelet(name)
                .apiVersion(kameletApiVersion)
                .namespace(namespace)
                .isAvailable());
    }

    private void initializeKameletBuilder() {
        kamelet = new KameletBuilder();
        definition = new DefinitionBuilder();
        kameletSpecBuilder = new KameletSpecBuilder();
        dataTypes = new HashMap<>();
        kameletTemplate = null;
    }
}
