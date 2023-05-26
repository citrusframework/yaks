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

import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.camelk.model.Pipe;
import org.citrusframework.yaks.camelk.model.PipeSpec;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletSpec;
import org.citrusframework.yaks.kafka.KafkaSettings;
import org.citrusframework.yaks.knative.KnativeSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;


public class KameletSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    private String kameletApiVersion = KameletSettings.getKameletApiVersion();

    // Kamelet builder
    private Kamelet.Builder kamelet;
    private KameletSpec.Definition definition;

    // Pipe endpoints
    private PipeSpec.Endpoint source;
    private PipeSpec.Endpoint sink;

    private Map<String, Object> sourceProperties;
    private Map<String, Object> sinkProperties;

    private String namespace = KameletSettings.getNamespace();

    private boolean autoRemoveResources = CamelKSettings.isAutoRemoveResources();
    private boolean supportVariablesInSources = CamelKSettings.isSupportVariablesInSources();

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        initializeKameletBuilder();
        initializePipeBuilder();
    }

    @Given("^Disable auto removal of Kamelet resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

    @Given("^Enable auto removal of Kamelet resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;
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
        if (format.contains(":")) {
            String[] schemeAndFormat = format.split(":");
            kamelet.addDataType(slot, schemeAndFormat[0], schemeAndFormat[1]);
        } else {
            kamelet.addDataType(slot, "camel", format);
        }
	}

    @Given("^Kamelet title \"(.+)\"$")
	public void setTitle(String title) {
        definition.setTitle(title);
	}

    @Given("^Kamelet source ([a-z0-9-]+).([a-z0-9-]+)$")
	public void setSource(String name, String language, String content) {
        kamelet.source(name, language, content);
	}

    @Given("^Kamelet template")
	public void setFlow(String template) {
        kamelet.template(template);
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
            definition.getRequired().add(propertyName);
        }

        definition.getProperties().put(propertyName,
                new KameletSpec.Definition.PropertyConfig(title, type, defaultValue, example));
	}

	@Given("^(?:Pipe|KameletBinding) source properties$")
    public void setPipeSourceProperties(Map<String, Object> properties) {
        this.sourceProperties.putAll(properties);
    }

    @Given("^(?:Pipe|KameletBinding) sink properties$")
    public void setPipeSinkProperties(Map<String, Object> properties) {
        this.sinkProperties.putAll(properties);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to uri ([^\\s]+)$")
    public void bindKameletToUri(String kameletName, String uri) {
        PipeSpec.Endpoint.ObjectReference sourceRef =
                new PipeSpec.Endpoint.ObjectReference(CamelKSupport.CAMELK_CRD_GROUP + "/" + kameletApiVersion, "Kamelet", namespace, kameletName);
        source = new PipeSpec.Endpoint(sourceRef);

        sink = new PipeSpec.Endpoint(uri);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Kafka topic ([^\\s]+)$")
    public void bindKameletToKafka(String kameletName, String topic) {
        PipeSpec.Endpoint.ObjectReference sourceRef =
                new PipeSpec.Endpoint.ObjectReference(CamelKSupport.CAMELK_CRD_GROUP + "/" + kameletApiVersion, "Kamelet", namespace, kameletName);
        source = new PipeSpec.Endpoint(sourceRef);

        PipeSpec.Endpoint.ObjectReference sinkRef =
                new PipeSpec.Endpoint.ObjectReference("KafkaTopic", KafkaSettings.getNamespace(), topic);
        sink = new PipeSpec.Endpoint(sinkRef);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative channel ([^\\s]+)$")
    public void bindKameletToKnativeChannel(String kameletName, String channel) {
        bindKameletToKnativeChannel(kameletName, channel, "InMemoryChannel");
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative channel ([^\\s]+) of kind ([^\\s]+)$")
    public void bindKameletToKnativeChannel(String kameletName, String channel, String channelKind) {
        PipeSpec.Endpoint.ObjectReference sourceRef =
                new PipeSpec.Endpoint.ObjectReference(CamelKSupport.CAMELK_CRD_GROUP + "/" + kameletApiVersion, "Kamelet", namespace, kameletName);
        source = new PipeSpec.Endpoint(sourceRef);

        PipeSpec.Endpoint.ObjectReference sinkRef =
                new PipeSpec.Endpoint.ObjectReference(channelKind, KnativeSettings.getNamespace(), channel);
        sink = new PipeSpec.Endpoint(sinkRef);
    }

    @Given("^load Kamelet ([a-z0-9-]+).kamelet.yaml$")
    public void loadKameletFromFile(String fileName) {
        Resource resource = new ClassPathResource(fileName + ".kamelet.yaml");
        runner.run(camelk()
                .client(k8sClient)
                .createKamelet(fileName)
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

    @Given("^load (?:Pipe|KameletBinding) ([a-z0-9-]+).yaml$")
    public void loadPipeFromFile(String fileName) {
        Resource resource = new ClassPathResource(fileName + ".yaml");
        runner.run(camelk()
                .client(k8sClient)
                .createPipe(fileName)
                .resource(resource));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deletePipe(fileName)
                                     .apiVersion(kameletApiVersion)));
        }
    }

    @Given("^(?:create|new) Kamelet ([a-z0-9-]+)$")
	public void createNewKamelet(String name) {
        kamelet.name(name);

        if (definition.getTitle() == null || definition.getTitle().isEmpty()) {
            definition.setTitle(StringUtils.capitalize(name));
        }

        kamelet.definition(definition);

        runner.run(camelk()
                    .client(k8sClient)
                    .createKamelet(name)
                    .supportVariables(supportVariablesInSources)
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
        kamelet.template(template);
        createNewKamelet(name);
	}

    @Given("^(?:create|new) (?:Pipe|KameletBinding) ([a-z0-9-]+)$")
    public void createNewPipe(String name) {
        Pipe.Builder pipe = new Pipe.Builder();
        pipe.name(name);

        source.getProperties().putAll(sourceProperties);
        sink.getProperties().putAll(sinkProperties);

        pipe.source(source);
        pipe.sink(sink);

        runner.run(camelk()
                .client(k8sClient)
                .createPipe(name)
                .apiVersion(kameletApiVersion)
                .fromBuilder(pipe));

        initializePipeBuilder();

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deletePipe(name)
                                     .apiVersion(kameletApiVersion)));
        }
    }

    @Given("^delete Kamelet ([a-z0-9-]+)$")
	public void deleteKamelet(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteKamelet(name)
                    .apiVersion(kameletApiVersion));
	}

    @Given("^delete (?:Pipe|KameletBinding) ([a-z0-9-]+)$")
	public void deletePipe(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deletePipe(name)
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

    @Given("^(?:Pipe|KameletBinding) ([a-z0-9-]+) is available$")
    @Then("^(?:Pipe|KameletBinding) ([a-z0-9-]+) should be available$")
    public void pipeShouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyPipe(name)
                .apiVersion(kameletApiVersion)
                .isAvailable());
    }

    private void initializeKameletBuilder() {
        kamelet = new Kamelet.Builder();
        definition = new KameletSpec.Definition();
    }

    private void initializePipeBuilder() {
        source = null;
        sink = null;
        sourceProperties = new HashMap<>();
        sinkProperties = new HashMap<>();
    }
}
