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

package org.citrusframework.yaks.jms;

import javax.jms.ConnectionFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.actions.ReceiveMessageAction;
import com.consol.citrus.actions.SendMessageAction;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.jms.endpoint.JmsEndpointBuilder;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.citrusframework.yaks.jms.connection.ConnectionFactoryCreator;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

public class JmsSteps {

    private static final long TIMEOUT = System.getenv("YAKS_JMS_TIMEOUT") != null ? Integer.parseInt(System.getenv("YAKS_JMS_TIMEOUT")) : TimeUnit.SECONDS.toMillis(60);

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private JmsEndpoint jmsEndpoint;

    private ConnectionFactory connectionFactory;

    private String selector = "";

    @Given("^(?:JMS|jms) connection factory$")
    public void setConnection(DataTable properties) throws ClassNotFoundException {
        List<List<String>> cells = properties.cells();
        Map<String, String> connectionSettings = new LinkedHashMap<>();
        cells.forEach(row -> connectionSettings.put(row.get(0), row.get(1)));

        connectionFactory = ConnectionFactoryCreator.lookup(connectionSettings.get("type"))
                                                    .create(connectionSettings);

        citrus.getCitrusContext().getReferenceResolver().bind("connectionFactory", connectionFactory);
    }

    @Given("^(?:JMS|jms) destination: (.+)$")
    public void jmsEndpoint(String destination) {
        jmsEndpoint = new JmsEndpointBuilder()
                .connectionFactory(connectionFactory)
                .destination(destination)
                .build();
    }

    @Given("^(?:JMS|jms) selector: (.+)$")
    public void selector(String selector) {
        this.selector = selector;
    }

    @When("^send message to JMS broker with body: (.+)")
    @Given("^message in JMS broker with body: (.+)$")
    public void sendMessageBody(String body) {
        sendToBroker(body, Collections.emptyMap());
    }

    @When("^send message to JMS broker with body")
    @Given("^message in JMS broker$")
    public void sendMessageBodyFull(String body) {
        sendToBroker(body, Collections.emptyMap());
    }

    @When("^send message to JMS broker with body and headers: (.+)")
    @Given("^message in JMS broker with body and headers: (.+)$")
    public void sendMessageBodyHeaders(String body, DataTable headers) {
        sendToBroker(body, headers.asMap(String.class, Object.class));
    }

    @Then("^(?:expect|verify) message in JMS broker with body: (.+)$")
    public void receiveMessageBody(String body) {
        receiveFromBroker(body, Collections.emptyMap());
    }

    @Then("^(?:expect|verify) message in JMS broker with body$")
    public void receiveMessageBodyFull(String body) {
        receiveFromBroker(body, Collections.emptyMap());
    }

    @Then("^(?:expect|verify) message in JMS broker with body and headers: (.+)$")
    public void receiveMessageBody(String body, DataTable headers) {
        receiveFromBroker(body, headers.asMap(String.class, Object.class));
    }

    private SendMessageAction sendToBroker(String body,  Map<String, Object> headers) {
        return runner.run(send().endpoint(jmsEndpoint).payload(body).headers(headers));
    }

    private ReceiveMessageAction receiveFromBroker(String body, Map<String,Object> headers) {
        return runner.run(receive().endpoint(jmsEndpoint).payload(body).headers(headers).selector(selector).timeout(TIMEOUT));
    }

}
