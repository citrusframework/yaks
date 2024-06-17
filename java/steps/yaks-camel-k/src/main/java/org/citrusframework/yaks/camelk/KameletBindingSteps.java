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
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.camel.v1alpha1.KameletBindingBuilder;
import org.apache.camel.v1alpha1.kameletbindingspec.SinkBuilder;
import org.apache.camel.v1alpha1.kameletbindingspec.SourceBuilder;
import org.apache.camel.v1alpha1.kameletbindingspec.source.Ref;
import org.apache.camel.v1alpha1.kameletbindingspec.source.RefBuilder;
import org.citrusframework.Citrus;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusFramework;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.spi.Resource;
import org.citrusframework.yaks.kafka.KafkaSettings;
import org.citrusframework.yaks.knative.KnativeSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.citrusframework.yaks.util.ResourceUtils;

import static org.citrusframework.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;

public class KameletBindingSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestContext context;

    private KubernetesClient k8sClient;

    // Binding endpoints
    private SourceBuilder source;
    private SinkBuilder sink;

    private Map<String, Object> sourceProperties;
    private Map<String, Object> sinkProperties;

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        initializeKameletBindingBuilder();
    }

    @Given("^KameletBinding source properties$")
    public void setKameletBindingSourceProperties(Map<String, Object> properties) {
        this.sourceProperties.putAll(properties);
    }

    @Given("^KameletBinding sink properties$")
    public void setKameletBindingSinkProperties(Map<String, Object> properties) {
        this.sinkProperties.putAll(properties);
    }

    @Given("^KameletBinding event source Kamelet ([a-z0-9-]+)$")
    public void setKameletEventSource(String kameletName) {
        Ref kameletRef = new RefBuilder()
                .withName(kameletName)
                .withApiVersion(CamelKSupport.CAMELK_CRD_GROUP + "/" + getKameletApiVersion())
                .withKind("Kamelet")
                .withNamespace(getNamespace())
                .build();
        source = new SourceBuilder().withRef(kameletRef);
    }

    @Given("^KameletBinding event sink uri ([^\\s]+)$")
    public void setEventSinkUri(String uri) {
        sink = new SinkBuilder().withUri(uri);
    }

    @Given("^KameletBinding event sink Kafka topic ([^\\s]+)$")
    public void setEventSinkKafkaTopic(String topic) {
        org.apache.camel.v1alpha1.kameletbindingspec.sink.Ref sinkRef =
                new org.apache.camel.v1alpha1.kameletbindingspec.sink.RefBuilder()
                        .withName(topic)
                        .withApiVersion("kafka.strimzi.io/" + KafkaSettings.getApiVersion())
                        .withKind("KafkaTopic")
                        .withNamespace(KafkaSettings.getNamespace())
                        .build();
        sink = new SinkBuilder().withRef(sinkRef);
    }

    @Given("^KameletBinding event sink Knative channel ([^\\s]+)$")
    public void setEventSinkKnativeChannel(String channel) {
        setEventSinkKnativeChannel(channel, "InMemoryChannel");
    }

    @Given("^KameletBinding event sink Knative channel ([^\\s]+) of kind ([^\\s]+)$")
    public void setEventSinkKnativeChannel(String channel, String channelKind) {
        org.apache.camel.v1alpha1.kameletbindingspec.sink.Ref sinkRef =
                new org.apache.camel.v1alpha1.kameletbindingspec.sink.RefBuilder()
                        .withName(channel)
                        .withApiVersion("messaging.knative.dev/" + KnativeSettings.getApiVersion())
                        .withKind(channelKind)
                        .withNamespace(KnativeSettings.getNamespace())
                        .build();
        sink = new SinkBuilder().withRef(sinkRef);
    }

    @Given("^KameletBinding event sink Knative broker ([^\\s]+)$")
    public void setEventSinkKnativeBroker(String broker) {
        org.apache.camel.v1alpha1.kameletbindingspec.sink.Ref sinkRef =
                new org.apache.camel.v1alpha1.kameletbindingspec.sink.RefBuilder()
                        .withName(broker)
                        .withApiVersion("eventing.knative.dev/" + KnativeSettings.getApiVersion())
                        .withKind("Broker")
                        .withNamespace(KnativeSettings.getNamespace())
                        .build();
        sink = new SinkBuilder().withRef(sinkRef);
    }

    @Given("^load KameletBinding ([a-z0-9-]+).yaml$")
    public void loadKameletBindingFromFile(String fileName) {
        Resource resource = ResourceUtils.resolve(fileName + ".yaml", context);
        runner.run(camelk()
                .client(k8sClient)
                .createKameletBinding(fileName)
                .resource(resource));

        if (isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deleteKameletBinding(fileName)));
        }
    }

    @Given("^(?:create|new) KameletBinding ([a-z0-9-]+)$")
    public void createNewKameletBinding(String name) {
        KameletBindingBuilder builder = new KameletBindingBuilder();

        builder.withNewMetadata()
                .withName(name)
            .endMetadata();

        source.editOrNewProperties().addToAdditionalProperties(sourceProperties);
        sink.editOrNewProperties().addToAdditionalProperties(sinkProperties);

        builder.withNewSpec()
                .withSource(source.build())
                .withSink(sink.build())
            .endSpec();

        runner.run(camelk()
                .client(k8sClient)
                .createKameletBinding(name)
                .fromBuilder(builder));

        initializeKameletBindingBuilder();

        if (isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deleteKameletBinding(name)));
        }
    }

    @Given("^delete KameletBinding ([a-z0-9-]+)$")
	public void deleteKameletBinding(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteKameletBinding(name));
	}

    @Given("^KameletBinding ([a-z0-9-]+) is available$")
    @Then("^KameletBinding ([a-z0-9-]+) should be available$")
    public void bindingShouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyKameletBinding(name)
                .isAvailable());
    }

    private void initializeKameletBindingBuilder() {
        source = null;
        sink = null;
        sourceProperties = new HashMap<>();
        sinkProperties = new HashMap<>();
    }

    private String getKameletApiVersion() {
        if (context.getVariables().containsKey(VariableNames.KAMELET_API_VERSION.value())) {
            return context.getVariable(VariableNames.KAMELET_API_VERSION.value());
        }

        return KameletSettings.getKameletApiVersion();
    }

    private String getNamespace() {
        if (context.getVariables().containsKey(VariableNames.KAMELET_NAMESPACE.value())) {
            return context.getVariable(VariableNames.KAMELET_NAMESPACE.value());
        }

        return KameletSettings.getNamespace();
    }

    private boolean isAutoRemoveResources() {
        if (context.getVariables().containsKey(VariableNames.AUTO_REMOVE_RESOURCES.value())) {
            return context.getVariable(VariableNames.AUTO_REMOVE_RESOURCES.value(), Boolean.class);
        }

        return CamelKSettings.isAutoRemoveResources();
    }
}
