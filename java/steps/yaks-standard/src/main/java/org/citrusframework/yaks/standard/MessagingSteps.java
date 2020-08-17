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

package org.citrusframework.yaks.standard;

import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.CitrusSettings;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.message.DefaultMessage;
import com.consol.citrus.message.Message;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

/**
 * @author Christoph Deppisch
 */
public class MessagingSteps {

    @CitrusResource
    private TestCaseRunner runner;

    /** Messages defined by id */
    private Map<String, Message> messages;

    @Before
    public void before() {
        messages = new HashMap<>();
    }

    @Given("^(?:create|new) message ([^\\s]+)$")
    public void message(String messageId) {
        messages.put(messageId, new DefaultMessage());
    }

    @When("^endpoint ([^\\s]+) sends message \\$([^\\s]+)$")
    @Then("^endpoint ([^\\s]+) should send message \\$([^\\s]+)$")
    public void sendMessage(final String endpoint, final String messageId) {
        if (messages.containsKey(messageId)) {
            runner.when(send()
                    .endpoint(endpoint)
                    .message(new DefaultMessage(messages.get(messageId))));
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find message for id '%s'", messageId));
        }
    }

    @When("^endpoint ([^\\s]+) sends payload ([\\w\\W]+)$")
    @Then("^endpoint ([^\\s]+) should send payload ([\\w\\W]+)$")
    public void sendPayload(final String endpoint, final String payload) {
        runner.when(send()
                .endpoint(endpoint)
                .payload(payload));
    }

    @When("^endpoint ([^\\s]+) sends payload$")
    @Then("^endpoint ([^\\s]+) should send payload$")
    public void sendMultilinePayload(String endpoint, String payload) {
        sendPayload(endpoint, payload);
    }

    @When("^endpoint ([^\\s]+) receives ([^\\s]+) message \\$([^\\s]+)$")
    @Then("^endpoint ([^\\s]+) should receive ([^\\s]+) message \\$([^\\s]+)$")
    public void receiveMessage(final String endpoint, final String type, final String messageId) {
        if (messages.containsKey(messageId)) {
            runner.when(receive()
                    .endpoint(endpoint)
                    .messageType(type)
                    .message(new DefaultMessage(messages.get(messageId))));
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find message for id '%s'", messageId));
        }
    }

    @When("^endpoint ([^\\s]+) receives message \\$([^\\s]+)$")
    @Then("^endpoint ([^\\s]+) should receive message \\$([^\\s]+)$")
    public void receiveMessage(final String endpoint, final String messageName) {
        receiveMessage(endpoint, CitrusSettings.DEFAULT_MESSAGE_TYPE, messageName);
    }

    @When("^endpoint ([^\\s]+) receives ([^\\s]+) payload ([\\w\\W]+)$")
    @Then("^endpoint ([^\\s]+) should receive ([^\\s]+) payload ([\\w\\W]+)$")
    public void receivePayload(final String endpoint, final String type, final String payload) {
        runner.when(receive()
                .endpoint(endpoint)
                .messageType(type)
                .payload(payload));
    }

    @When("^endpoint ([^\\s]+) receives payload ([\\w\\W]+)$")
    @Then("^endpoint ([^\\s]+) should receive payload ([\\w\\W]+)$")
    public void receiveDefault(String endpoint, String payload) {
        receivePayload(endpoint, CitrusSettings.DEFAULT_MESSAGE_TYPE, payload);
    }

    @When("^endpoint ([^\\s]+) receives payload$")
    @Then("^endpoint ([^\\s]+) should receive payload$")
    public void receiveMultilinePayload(String endpoint, String payload) {
        receivePayload(endpoint, CitrusSettings.DEFAULT_MESSAGE_TYPE, payload);
    }

    @When("^endpoint ([^\\s]+) receives ([^\\s]+) payload$")
    @Then("^endpoint ([^\\s]+) should receive ([^\\s]+) payload$")
    public void shouldReceiveMultiline(String endpoint, String type, String payload) {
        receivePayload(endpoint, type, payload);
    }

    @And("^\\$([^\\s]+) header ([^\\s]+)(?: is |=)\"([^\"]*)\"$")
    public void addHeader(String messageId, String name, String value) {
        messages.get(messageId).setHeader(name, value);
    }

    @And("^\\$([^\\s]+) has payload ([\\w\\W]+)$")
    public void addPayload(String messageId, String payload) {
        messages.get(messageId).setPayload(payload);
    }

    @And("^\\$([^\\s]+) has payload$")
    public void addPayloadMultiline(String messageId, String payload) {
        addPayload(messageId, payload);
    }
}
