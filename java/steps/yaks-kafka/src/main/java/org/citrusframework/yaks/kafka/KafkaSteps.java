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

package org.citrusframework.yaks.kafka;

import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.kafka.endpoint.KafkaEndpoint;
import com.consol.citrus.kafka.endpoint.KafkaEndpointBuilder;
import com.consol.citrus.kafka.message.KafkaMessage;
import com.consol.citrus.kafka.message.KafkaMessageHeaders;
import com.consol.citrus.message.Message;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

public class KafkaSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private Map<String, Object> headers = new HashMap<>();
    private String body;

    private KafkaEndpoint kafkaEndpoint;

    private String messageKey;
    private Integer partition;
    private String topic = "test";

    private long timeout = KafkaSettings.getConsumerTimeout();

    private static final String KAFKA_ENDPOINT_NAME = "kafkaEndpoint";

    @Before
    public void before(Scenario scenario) {
        if (kafkaEndpoint == null) {
            if (citrus.getCitrusContext().getReferenceResolver().resolveAll(KafkaEndpoint.class).size() == 1L) {
                kafkaEndpoint = citrus.getCitrusContext().getReferenceResolver().resolve(KafkaEndpoint.class);
            } else if (citrus.getCitrusContext().getReferenceResolver().isResolvable(KAFKA_ENDPOINT_NAME)) {
                kafkaEndpoint = citrus.getCitrusContext().getReferenceResolver().resolve(KAFKA_ENDPOINT_NAME, KafkaEndpoint.class);
            } else {
                kafkaEndpoint = new KafkaEndpointBuilder().build();
                citrus.getCitrusContext().getReferenceResolver().bind(KAFKA_ENDPOINT_NAME, kafkaEndpoint);
            }
        }

        headers = new HashMap<>();
        body = null;

        messageKey = null;
        partition = null;
    }

    @Given("^(?:K|k)afka connection$")
    public void setConnection(DataTable properties) {
        Map<String, String> connectionProps = properties.asMap(String.class, String.class);

        String url = connectionProps.getOrDefault("url", "localhost:9092");
        String topicName = connectionProps.getOrDefault("topic", this.topic);
        String consumerGroup = connectionProps.getOrDefault("consumerGroup", KafkaMessageHeaders.KAFKA_PREFIX + "group");
        String offsetReset = connectionProps.getOrDefault("offsetReset", "earliest");

        setTopic(topicName);
        kafkaEndpoint.getEndpointConfiguration().setServer(url);
        kafkaEndpoint.getEndpointConfiguration().setOffsetReset(offsetReset);
        kafkaEndpoint.getEndpointConfiguration().setConsumerGroup(consumerGroup);
    }

    @Given("^(?:K|k)afka producer configuration$")
    public void setProducerConfig(DataTable properties) {
        Map<String, Object> producerProperties = properties.asMap(String.class, Object.class);
        kafkaEndpoint.getEndpointConfiguration().setProducerProperties(producerProperties);
    }

    @Given("^(?:K|k)afka consumer configuration$")
    public void setConsumerConfig(DataTable properties) {
        Map<String, Object> consumerProperties = properties.asMap(String.class, Object.class);
        kafkaEndpoint.getEndpointConfiguration().setConsumerProperties(consumerProperties);
    }

    @Given("^(?:K|k)afka message key: (.+)$")
    public void setMessageKey(String key) {
        this.messageKey = key;
    }

    @Given("^(?:K|k)afka consumer timeout is (\\d+)(?: ms| milliseconds)$")
    public void setConsumerTimeout(int milliseconds) {
        this.timeout = milliseconds;
    }

    @Given("^(?:K|k)afka topic partition: (\\d+)$")
    public void setPartition(int partition) {
        this.partition = partition;
    }

    @Given("^(?:K|k)afka topic: (.+)$")
    public void setTopic(String topicName) {
        this.topic = topicName;
        kafkaEndpoint.getEndpointConfiguration().setTopic(topicName);
    }

    @Given("^(?:K|k)afka message header ([^\\s]+)(?:=| is )\"(.+)\"$")
    @Then("^(?:expect|verify) (?:K|k)afka message header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addMessageHeader(String name, Object value) {
        headers.put(name, value);
    }

    @Given("^(?:K|k)afka message headers$")
    public void addMessageHeaders(DataTable headers) {
        Map<String, Object> headerPairs = headers.asMap(String.class, Object.class);
        headerPairs.forEach(this::addMessageHeader);
    }

    @Given("^(?:K|k)afka message body$")
    @Then("^(?:expect|verify) (?:K|k)afka message body$")
    public void setMessageBodyMultiline(String body) {
        setMessageBody(body);
    }

    @Given("^(?:K|k)afka message body: (.+)$")
    @Then("^(?:expect|verify) (?:K|k)afka message body: (.+)$")
    public void setMessageBody(String body) {
        this.body = body;
    }

    @When("^send (?:K|k)afka message$")
    public void sendMessage() {
        runner.run(send().endpoint(kafkaEndpoint)
                .message(createKafkaMessage()));

        body = null;
        headers.clear();
    }

    @Then("^receive (?:K|k)afka message$")
    public void receiveMessage() {
        runner.run(receive().endpoint(kafkaEndpoint)
                .timeout(timeout)
                .message(createKafkaMessage()));

        body = null;
        headers.clear();
    }

    @When("^send (?:K|k)afka message to topic (.+)$")
    public void sendMessage(String topicName) {
        setTopic(topicName);
        sendMessage();
    }

    @Then("^receive (?:K|k)afka message on topic (.+)")
    public void receiveMessage(String topicName) {
        setTopic(topicName);
        receiveMessage();
    }

    @When("^send (?:K|k)afka message with body and headers: (.+)$")
    @Given("^message in (?:K|k)afka with body and headers: (.+)$")
    public void sendMessageBodyAndHeaders(String body, DataTable headers) {
        setMessageBody(body);
        addMessageHeaders(headers);
        sendMessage();
    }

    @When("^send (?:K|k)afka message with body: (.+)$")
    @Given("^message in (?:K|k)afka with body: (.+)$")
    public void sendMessageBody(String body) {
        setMessageBody(body);
        sendMessage();
    }

    @When("^send (?:K|k)afka message with body$")
    @Given("^message in (?:K|k)afka with body$")
    public void sendMessageBodyMultiline(String body) {
        sendMessageBody(body);
    }

    @Then("^(?:expect|verify) (?:K|k)afka message with body and headers: (.+)$")
    public void receiveFromKafka(String body, DataTable headers) {
        setMessageBody(body);
        addMessageHeaders(headers);
        receiveMessage();
    }

    @Then("^(?:expect|verify) (?:K|k)afka message with body: (.+)$")
    public void receiveMessageBody(String body) {
        setMessageBody(body);
        receiveMessage();
    }

    @Then("^(?:expect|verify) (?:K|k)afka message with body$")
    public void receiveMessageBodyMultiline(String body) {
        receiveMessageBody(body);
    }

    private Message createKafkaMessage() {
        KafkaMessage message = new KafkaMessage(body, headers)
                .topic(topic);

        if (messageKey != null) {
            message.messageKey(messageKey);
        }

        if (partition != null) {
            message.partition(partition);
        }
        return message;
    }

}
