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
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletBinding;
import org.citrusframework.yaks.camelk.model.KameletBindingSpec;
import org.citrusframework.yaks.camelk.model.KameletSpec;
import org.citrusframework.yaks.kafka.KafkaSettings;
import org.citrusframework.yaks.knative.KnativeSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;


public class KameletSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    // Kamelet builder
    private Kamelet.Builder kamelet;
    private KameletSpec.Definition definition;

    // KameleteBinding builder
    private KameletBinding.Builder binding;
    private KameletBindingSpec.Endpoint source;
    private KameletBindingSpec.Endpoint sink;

    private Map<String, Object> sourceProperties;
    private Map<String, Object> sinkProperties;

    private boolean autoRemoveResources = CamelKSettings.isAutoRemoveResources();

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        initializeKameletBuilder();
        initializeKameletBindingBuilder();
    }

    @Given("^Disable auto removal of Kamelet resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

    @Given("^Enable auto removal of Kamelet resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;
    }

	@Given("^Kamelet type (in|out|error)(?:=| is )\"(.+)\"$")
	public void addType(String slot, String mediaType) {
        kamelet.addType(slot, mediaType);
	}

    @Given("^Kamelet title \"(.+)\"$")
	public void setTitle(String title) {
        definition.setTitle(title);
	}

    @Given("^Kamelet source ([a-z0-9-]+).([a-z0-9-]+)$")
	public void setSource(String name, String language, String content) {
        kamelet.source(name, language, content);
	}

    @Given("^Kamelet flow$")
	public void setFlow(String flow) {
        kamelet.flow(flow);
	}

    @Given("^Kamelet property definition$")
	public void addPropertyDefinition(Map<String, Object> propertyConfiguration) {
        if (!propertyConfiguration.containsKey("name")) {
            throw new CitrusRuntimeException("Missing property name in configuration. " +
                    "Please add the property name to the property definition");
        }

        String propertyName = propertyConfiguration.get("name").toString();
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

	@Given("^KameletBinding source properties$")
    public void setKameletBindingSourceProperties(Map<String, Object> properties) {
        this.sourceProperties.putAll(properties);
    }

    @Given("^KameletBinding sink properties$")
    public void setKameletBindingSinkProperties(Map<String, Object> properties) {
        this.sinkProperties.putAll(properties);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to uri ([^\\s]+)$")
    public void bindKameletToUri(String kameletName, String uri) {
        KameletBindingSpec.Endpoint.ObjectReference sourceRef =
                new KameletBindingSpec.Endpoint.ObjectReference(kameletName, "Kamelet", CamelKSettings.getNamespace());
        source = new KameletBindingSpec.Endpoint(sourceRef);

        sink = new KameletBindingSpec.Endpoint(uri);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Kafka topic ([^\\s]+)$")
    public void bindKameletToKafka(String kameletName, String topic) {
        KameletBindingSpec.Endpoint.ObjectReference sourceRef =
                new KameletBindingSpec.Endpoint.ObjectReference(kameletName, "Kamelet", CamelKSettings.getNamespace());
        source = new KameletBindingSpec.Endpoint(sourceRef);

        KameletBindingSpec.Endpoint.ObjectReference sinkRef =
                new KameletBindingSpec.Endpoint.ObjectReference(topic, "KafkaTopic", KafkaSettings.getNamespace());
        sink = new KameletBindingSpec.Endpoint(sinkRef);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative channel ([^\\s]+)$")
    public void bindKameletToKnativeChannel(String kameletName, String channel) {
        bindKameletToKnativeChannel(kameletName, channel, "InMemoryChannel");
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative channel ([^\\s]+) of kind ([^\\s]+)$")
    public void bindKameletToKnativeChannel(String kameletName, String channel, String channelKind) {
        KameletBindingSpec.Endpoint.ObjectReference sourceRef =
                new KameletBindingSpec.Endpoint.ObjectReference(kameletName, "Kamelet", CamelKSettings.getNamespace());
        source = new KameletBindingSpec.Endpoint(sourceRef);

        KameletBindingSpec.Endpoint.ObjectReference sinkRef =
                new KameletBindingSpec.Endpoint.ObjectReference(channel, channelKind, KnativeSettings.getNamespace());
        sink = new KameletBindingSpec.Endpoint(sinkRef);
    }

    @Given("^load Kamelet ([a-z0-9-]+).kamelet.yaml$")
    public void loadKameletFromFile(String fileName) {
        Resource resource = new ClassPathResource(fileName + ".kamelet.yaml");
        runner.run(camelk()
                .client(k8sClient)
                .createKamelet(fileName)
                .resource(resource));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteKamelet(fileName)));
        }
    }

    @Given("^load KameletBinding ([a-z0-9-]+).yaml$")
    public void loadKameletBindingFromFile(String fileName) {
        Resource resource = new ClassPathResource(fileName + ".yaml");
        runner.run(camelk()
                .client(k8sClient)
                .createKameletBinding(fileName)
                .resource(resource));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteKameletBinding(fileName)));
        }
    }

    @Given("^create Kamelet ([a-z0-9-]+)$")
	public void createNewKamelet(String name) {
        kamelet.name(name);

        if (definition.getTitle() == null || definition.getTitle().isEmpty()) {
            definition.setTitle(StringUtils.capitalize(name));
        }

        kamelet.definition(definition);

        runner.run(camelk()
                    .client(k8sClient)
                    .createKamelet(name)
                    .fromBuilder(kamelet));

        initializeKameletBuilder();

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteKamelet(name)));
        }
	}

	@Given("^create Kamelet ([a-z0-9-]+) with flow$")
	public void createNewKameletWithFlow(String name, String flow) {
        kamelet.flow(flow);
        createNewKamelet(name);
	}

    @Given("^create KameletBinding ([a-z0-9-]+)$")
    public void createNewKameletBinding(String name) {
        binding.name(name);

        source.getProperties().putAll(sourceProperties);
        sink.getProperties().putAll(sinkProperties);

        binding.source(source);
        binding.sink(sink);

        runner.run(camelk()
                .client(k8sClient)
                .createKameletBinding(name)
                .fromBuilder(binding));

        initializeKameletBindingBuilder();

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteKameletBinding(name)));
        }
    }

    @Given("^delete Kamelet ([a-z0-9-]+)$")
	public void deleteKamelet(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteKamelet(name));
	}

    @Given("^delete KameletBinding ([a-z0-9-]+)$")
	public void deleteKameletBinding(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteKameletBinding(name));
	}

    @Given("^Kamelet ([a-z0-9-]+) is available$")
    @Then("^Kamelet ([a-z0-9-]+) should be available$")
    public void kameletShouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyKamelet(name)
                .isAvailable());
    }

    @Given("^KameletBinding ([a-z0-9-]+) is available$")
    @Then("^KameletBinding ([a-z0-9-]+) should be available$")
    public void kameletBindingShouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyKameletBinding(name)
                .isAvailable());
    }

    private void initializeKameletBuilder() {
        kamelet = new Kamelet.Builder();
        definition = new KameletSpec.Definition();
    }

    private void initializeKameletBindingBuilder() {
        binding = new KameletBinding.Builder();
        source = null;
        sink = null;
        sourceProperties = new HashMap<>();
        sinkProperties = new HashMap<>();
    }
}
