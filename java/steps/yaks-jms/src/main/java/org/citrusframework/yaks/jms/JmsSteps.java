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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.jms.endpoint.JmsEndpointBuilder;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.citrusframework.yaks.jms.connection.ConnectionFactoryCreator;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

public class JmsSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private Map<String, Object> headers = new HashMap<>();
    private String body;

    private JmsEndpoint jmsEndpoint;

    private ConnectionFactory connectionFactory;

    private String selector = "";

    private String endpointName = JmsSettings.getEndpointName();

    private long timeout = JmsSettings.getTimeout();

    @Before
    public void before(Scenario scenario) {
        if (jmsEndpoint == null) {
            if (citrus.getCitrusContext().getReferenceResolver().resolveAll(JmsEndpoint.class).size() == 1L) {
                jmsEndpoint = citrus.getCitrusContext().getReferenceResolver().resolve(JmsEndpoint.class);
            } else if (citrus.getCitrusContext().getReferenceResolver().isResolvable(endpointName)) {
                jmsEndpoint = citrus.getCitrusContext().getReferenceResolver().resolve(endpointName, JmsEndpoint.class);
            } else {
                jmsEndpoint = new JmsEndpointBuilder()
                        .timeout(timeout)
                        .build();
                citrus.getCitrusContext().getReferenceResolver().bind(endpointName, jmsEndpoint);
            }
        }

        if (connectionFactory == null
                && citrus.getCitrusContext().getReferenceResolver().resolveAll(ConnectionFactory.class).size() == 1L) {
            connectionFactory = citrus.getCitrusContext().getReferenceResolver().resolve(ConnectionFactory.class);

            if (jmsEndpoint.getEndpointConfiguration().getConnectionFactory() == null) {
                jmsEndpoint.getEndpointConfiguration().setConnectionFactory(connectionFactory);
            }
        }

        headers = new HashMap<>();
        body = null;
    }

    @Given("^(?:JMS|jms) connection factory ([^\\s]+)$")
    public void setConnectionFactory(String name) {
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            connectionFactory = citrus.getCitrusContext().getReferenceResolver().resolve(name, ConnectionFactory.class);
            jmsEndpoint.getEndpointConfiguration().setConnectionFactory(connectionFactory);
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find connection factory '%s'", name));
        }
    }

    @Given("^(?:JMS|jms) connection factory$")
    public void setConnection(DataTable properties) throws ClassNotFoundException {
        List<List<String>> cells = properties.cells();
        Map<String, String> connectionSettings = new LinkedHashMap<>();
        cells.forEach(row -> connectionSettings.put(row.get(0), row.get(1)));

        connectionFactory = ConnectionFactoryCreator.lookup(connectionSettings.get("type"))
                                                    .create(connectionSettings);

        citrus.getCitrusContext().getReferenceResolver().bind("connectionFactory", connectionFactory);
        jmsEndpoint.getEndpointConfiguration().setConnectionFactory(connectionFactory);
    }

    @Given("^(?:JMS|jms) destination: ([^\\s]+)$")
    public void setDestination(String destination) {
        jmsEndpoint.getEndpointConfiguration().setDestinationName(destination);
    }

    @Given("^(?:JMS|jms) endpoint \"([^\"\\s]+)\"$")
    public void setEndpoint(String name) {
        this.endpointName = name;
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            jmsEndpoint = citrus.getCitrusContext().getReferenceResolver().resolve(name, JmsEndpoint.class);
        } else if (jmsEndpoint != null) {
            citrus.getCitrusContext().getReferenceResolver().bind(endpointName, jmsEndpoint);
            jmsEndpoint.setName(endpointName);
        }
    }

    @Given("^(?:JMS|jms) selector: (.+)$")
    public void selector(String selector) {
        this.selector = selector;
    }

    @Given("^(?:JMS|jms) consumer timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Given("^(?:JMS|jms) message header ([^\\s]+)(?:=| is )\"(.+)\"$")
    @Then("^(?:expect|verify) (?:JMS|jms) message header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addMessageHeader(String name, Object value) {
        headers.put(name, value);
    }

    @Given("^(?:JMS|jms) message headers$")
    public void addMessageHeaders(DataTable headers) {
        Map<String, Object> headerPairs = headers.asMap(String.class, Object.class);
        headerPairs.forEach(this::addMessageHeader);
    }

    @Given("^(?:JMS|jms) message body$")
    @Then("^(?:expect|verify) (?:JMS|jms) message body$")
    public void setMessageBodyMultiline(String body) {
        setMessageBody(body);
    }

    @Given("^(?:JMS|jms) message body: (.+)$")
    @Then("^(?:expect|verify) (?:JMS|jms) message body: (.+)$")
    public void setMessageBody(String body) {
        this.body = body;
    }

    @When("^send (?:JMS|jms) message with body: (.+)$")
    @Given("^(?:JMS|jms) message with body: (.+)$")
    public void sendMessageBody(String body) {
        setMessageBody(body);
        sendMessage();
    }

    @When("^send (?:JMS|jms) message with body$")
    @Given("^(?:JMS|jms) message with body$")
    public void sendMessageBodyMultiline(String body) {
        sendMessageBody(body);
    }

    @When("^send (?:JMS|jms) message with body and headers: (.+)$")
    @Given("^(?:JMS|jms) message with body and headers: (.+)$")
    public void sendMessageBodyAndHeaders(String body, DataTable headers) {
        setMessageBody(body);
        addMessageHeaders(headers);
        sendMessage();
    }

    @Then("^(?:receive|expect|verify) (?:JMS|jms) message with body: (.+)$")
    public void receiveMessageBody(String body) {
        setMessageBody(body);
        receiveMessage();
    }

    @Then("^(?:receive|expect|verify) (?:JMS|jms) message with body$")
    public void receiveMessageBodyMultiline(String body) {
        receiveMessageBody(body);
    }

    @Then("^(?:receive|expect|verify) (?:JMS|jms) message with body and headers: (.+)$")
    public void receiveFromJms(String body, DataTable headers) {
        setMessageBody(body);
        addMessageHeaders(headers);
        receiveMessage();
    }

    @When("^send (?:JMS|jms) message$")
    public void sendMessage() {
        runner.run(send().endpoint(jmsEndpoint)
                .payload(body)
                .headers(headers));

        body = null;
        headers.clear();
    }

    @Then("^receive (?:JMS|jms) message$")
    public void receiveMessage() {
        runner.run(receive().endpoint(jmsEndpoint)
                .selector(selector)
                .timeout(timeout)
                .payload(body)
                .headers(headers));

        body = null;
        headers.clear();
    }

    @When("^send (?:JMS|jms) message to destination (.+)$")
    public void sendMessage(String destination) {
        setDestination(destination);
        sendMessage();
    }

    @Then("^receive (?:JMS|jms) message on destination (.+)")
    public void receiveMessage(String destination) {
        setDestination(destination);
        receiveMessage();
    }
}
