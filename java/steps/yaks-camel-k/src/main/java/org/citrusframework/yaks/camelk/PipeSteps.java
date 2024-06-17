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
import org.apache.camel.v1.PipeBuilder;
import org.apache.camel.v1.pipespec.SinkBuilder;
import org.apache.camel.v1.pipespec.SourceBuilder;
import org.apache.camel.v1.pipespec.source.Ref;
import org.apache.camel.v1.pipespec.source.RefBuilder;
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

public class PipeSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestContext context;

    private KubernetesClient k8sClient;

    // Pipe endpoints
    private SourceBuilder source;
    private SinkBuilder sink;

    private Map<String, Object> sourceProperties;
    private Map<String, Object> sinkProperties;

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }

        initializePipeBuilder();
    }

    @Given("^Pipe source properties$")
    public void setPipeSourceProperties(Map<String, Object> properties) {
        this.sourceProperties.putAll(properties);
    }

    @Given("^Pipe sink properties$")
    public void setPipeSinkProperties(Map<String, Object> properties) {
        this.sinkProperties.putAll(properties);
    }

    @Given("^Pipe event source Kamelet ([a-z0-9-]+)$")
    public void setKameletEventSource(String kameletName) {
        Ref kameletRef = new RefBuilder()
                .withName(kameletName)
                .withApiVersion(CamelKSupport.CAMELK_CRD_GROUP + "/" + getKameletApiVersion())
                .withKind("Kamelet")
                .withNamespace(getNamespace())
                .build();
        source = new SourceBuilder().withRef(kameletRef);
    }

    @Given("^Pipe event sink uri ([^\\s]+)$")
    public void setEventSinkUri(String uri) {
        sink = new SinkBuilder().withUri(uri);
    }

    @Given("^Pipe event sink Kafka topic ([^\\s]+)$")
    public void setEventSinkKafkaTopic(String topic) {
        org.apache.camel.v1.pipespec.sink.Ref sinkRef =
                new org.apache.camel.v1.pipespec.sink.RefBuilder()
                        .withName(topic)
                        .withApiVersion("kafka.strimzi.io/" + KafkaSettings.getApiVersion())
                        .withKind("KafkaTopic")
                        .withNamespace(KafkaSettings.getNamespace())
                        .build();
        sink = new SinkBuilder().withRef(sinkRef);
    }

    @Given("^Pipe event sink Knative channel ([^\\s]+)$")
    public void setEventSinkKnativeChannel(String channel) {
        setEventSinkKnativeChannel(channel, "InMemoryChannel");
    }

    @Given("^Pipe event sink Knative channel ([^\\s]+) of kind ([^\\s]+)$")
    public void setEventSinkKnativeChannel(String channel, String channelKind) {
        org.apache.camel.v1.pipespec.sink.Ref sinkRef =
                new org.apache.camel.v1.pipespec.sink.RefBuilder()
                        .withName(channel)
                        .withApiVersion("messaging.knative.dev/" + KnativeSettings.getApiVersion())
                        .withKind(channelKind)
                        .withNamespace(KnativeSettings.getNamespace())
                        .build();
        sink = new SinkBuilder().withRef(sinkRef);
    }

    @Given("^Pipe event sink Knative broker ([^\\s]+)$")
    public void setEventSinkKnativeBroker(String broker) {
        org.apache.camel.v1.pipespec.sink.Ref sinkRef =
                new org.apache.camel.v1.pipespec.sink.RefBuilder()
                        .withName(broker)
                        .withApiVersion("eventing.knative.dev/" + KnativeSettings.getApiVersion())
                        .withKind("Broker")
                        .withNamespace(KnativeSettings.getNamespace())
                        .build();
        sink = new SinkBuilder().withRef(sinkRef);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to uri ([^\\s]+)$")
    public void bindKameletToUri(String kameletName, String uri) {
        setKameletEventSource(kameletName);
        setEventSinkUri(uri);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Kafka topic ([^\\s]+)$")
    public void bindKameletToKafka(String kameletName, String topic) {
        setKameletEventSource(kameletName);
        setEventSinkKafkaTopic(topic);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative channel ([^\\s]+)$")
    public void bindKameletToKnativeChannel(String kameletName, String channel) {
        bindKameletToKnativeChannel(kameletName, channel, "InMemoryChannel");
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative channel ([^\\s]+) of kind ([^\\s]+)$")
    public void bindKameletToKnativeChannel(String kameletName, String channel, String channelKind) {
        setKameletEventSource(kameletName);
        setEventSinkKnativeChannel(channel, channelKind);
    }

    @Given("^bind Kamelet ([a-z0-9-]+) to Knative broker ([^\\s]+)$")
    public void bindKameletToKnativeBroker(String kameletName, String broker) {
        setKameletEventSource(kameletName);
        setEventSinkKnativeBroker(broker);
    }

    @Given("^load Pipe ([a-z0-9-]+).yaml$")
    public void loadPipeFromFile(String fileName) {
        Resource resource = ResourceUtils.resolve(fileName + ".yaml", context);
        runner.run(camelk()
                .client(k8sClient)
                .createPipe(fileName)
                .resource(resource));

        if (isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deletePipe(fileName)));
        }
    }

    @Given("^(?:create|new) Pipe ([a-z0-9-]+)$")
    public void createNewPipe(String name) {
        PipeBuilder pipe = new PipeBuilder();

        pipe.withNewMetadata()
                .withName(name)
            .endMetadata();

        source.editOrNewProperties().addToAdditionalProperties(sourceProperties);
        sink.editOrNewProperties().addToAdditionalProperties(sinkProperties);

        pipe.withNewSpec()
                .withSource(source.build())
                .withSink(sink.build())
            .endSpec();

        runner.run(camelk()
                .client(k8sClient)
                .createPipe(name)
                .fromBuilder(pipe));

        initializePipeBuilder();

        if (isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient)
                                     .deletePipe(name)));
        }
    }

    @Given("^delete Pipe ([a-z0-9-]+)$")
	public void deletePipe(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deletePipe(name));
	}

    @Given("^Pipe ([a-z0-9-]+) is available$")
    @Then("^Pipe ([a-z0-9-]+) should be available$")
    public void pipeShouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyPipe(name)
                .isAvailable());
    }

    private void initializePipeBuilder() {
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
