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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.kafka.endpoint.KafkaEndpoint;
import com.consol.citrus.kafka.endpoint.KafkaEndpointBuilder;
import com.consol.citrus.kafka.message.KafkaMessage;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

public class KafkaSteps {

    private static final long TIMEOUT = System.getenv("YAKS_KAFKA_TIMEOUT") != null ? Integer.parseInt(System.getenv("YAKS_KAFKA_TIMEOUT")) : TimeUnit.SECONDS.toMillis(60);

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KafkaEndpoint kafka;

    @Given("^(?:K|k)afka connection$")
    public void setConnection(DataTable properties) {
        Map<String, String> connectionProps = properties.asMap(String.class, String.class);

        String url = connectionProps.getOrDefault("url", "localhost:9092");
        String topic = connectionProps.getOrDefault("topic", "test");

        KafkaEndpointBuilder builder = new KafkaEndpointBuilder()
                .server(url)
                .topic(topic);

        kafka = builder.build();
    }

    @When("^send message to Kafka with body and headers: (.+)")
    @Given("^message in Kafka with body and headers: (.+)$")
    public void sendToKafka(String body, DataTable headers) {
       toKafka(body, headers.asMap(String.class, Object.class));
    }

    @When("^send message to Kafka with body: (.+)")
    @Given("^message in Kafka with body: (.+)$")
    public void sendToKafka(String body) {
        toKafka(body, Collections.emptyMap());
    }

    @When("^send message to Kafka with body")
    @Given("^message in Kafka with body$")
    public void sendToKafkaFull(String body) {
        sendToKafka(body);
    }

    @Then("^(?:expect|verify) message in Kafka with body: (.+)$")
    public void receiveFromKafka(String body) {
        fromKafka(body, Collections.emptyMap());
    }

    @Then("^(?:expect|verify) message in Kafka with body$")
    public void receiveFromKafkaFull(String body) {
        receiveFromKafka(body);
    }

    @Then("^(?:expect|verify) message in Kafka with body and headers: (.+)$")
    public void receiveFromKafka(String body, DataTable headers) {
        fromKafka(body, headers.asMap(String.class, Object.class));
    }

    private void toKafka(String body, Map<String,Object> headers) {
        runner.run(send().endpoint(kafka).message(new KafkaMessage(body, headers)));
    }

    private void fromKafka(String body, Map<String,Object> headers) {
       runner.run(receive().endpoint(kafka).timeout(TIMEOUT).message(new KafkaMessage(body, headers)));
    }

}
