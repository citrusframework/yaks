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

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.actions.ReceiveMessageAction;
import com.consol.citrus.actions.SendMessageAction;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.kafka.endpoint.KafkaEndpoint;
import com.consol.citrus.kafka.endpoint.KafkaEndpointBuilder;
import com.consol.citrus.kafka.message.KafkaMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class KafkaSteps {

    private static final long TIMEOUT = System.getenv("YAKS_KAFKA_TIMEOUT") != null ? Integer.valueOf(System.getenv("YAKS_KAFKA_TIMEOUT")) : TimeUnit.SECONDS.toMillis(60);

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KafkaEndpoint kafka;


    @Given("^(?:K|k)afka connection$")
    public void setConnection(DataTable properties) {
        Map<String, String> connectionProps = properties.asMap(String.class, String.class);

        String url = connectionProps.getOrDefault("url", "");
        String topic = connectionProps.getOrDefault("topic", "test");

        KafkaEndpointBuilder builder = CitrusEndpoints.kafka()
                .asynchronous()
                .server(url)
                .topic(topic);

        kafka = builder.build();
    }

    @When("^send message to Kafka with body and headers: (.+)")
    @Given("^message in Kafka with body and headers: (.+)$")
    public void sendToKafkaWithHeaders(String body, DataTable headers) {
       toKafka(body, headers.asMap(String.class, Object.class));
    }


    @When("^send message to Kafka with body: (.+)")
    @Given("^message in Kafka with body: (.+)$")
    public void sendToKafkaWithHeaders(String body) {
        toKafka(body, Collections.emptyMap());
    }

    @Then("^expect message in Kafka with body: (.+)$")
    public void receiveFromKafka(String body) {
        fromKafka(body, Collections.emptyMap());
    }

    @Then("^expect message in Kafka with body and headers: (.+)$")
    public void receiveFromKafka(String body, DataTable headers) {
        fromKafka(body, headers.asMap(String.class, Object.class));
    }

    private SendMessageAction toKafka(String body, Map<String,Object> headers) {
        return runner.run(send().endpoint(kafka).message(new KafkaMessage(body, headers)));
    }

    private ReceiveMessageAction fromKafka(String body, Map<String,Object> headers) {
       return runner.run(receive().endpoint(kafka).timeout(TIMEOUT).message(new KafkaMessage(body, headers)));
    }

}
