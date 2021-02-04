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

    @When("^endpoint ([^\\s]+) sends body ([\\w\\W]+)$")
    @Then("^endpoint ([^\\s]+) should send body ([\\w\\W]+)$")
    public void sendBody(final String endpoint, final String body) {
        runner.when(send()
                .endpoint(endpoint)
                .message()
                .body(body));
    }

    @When("^endpoint ([^\\s]+) sends body$")
    @Then("^endpoint ([^\\s]+) should send body$")
    public void sendMultilineBody(String endpoint, String body) {
        sendBody(endpoint, body);
    }

    @When("^endpoint ([^\\s]+) receives ([^\\s]+) message \\$([^\\s]+)$")
    @Then("^endpoint ([^\\s]+) should receive ([^\\s]+) message \\$([^\\s]+)$")
    public void receiveMessage(final String endpoint, final String type, final String messageId) {
        if (messages.containsKey(messageId)) {
            runner.when(receive()
                    .endpoint(endpoint)
                    .message(new DefaultMessage(messages.get(messageId))
                                    .setType(type)));
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find message for id '%s'", messageId));
        }
    }

    @When("^endpoint ([^\\s]+) receives message \\$([^\\s]+)$")
    @Then("^endpoint ([^\\s]+) should receive message \\$([^\\s]+)$")
    public void receiveMessage(final String endpoint, final String messageName) {
        receiveMessage(endpoint, CitrusSettings.DEFAULT_MESSAGE_TYPE, messageName);
    }

    @When("^endpoint ([^\\s]+) receives ([^\\s]+) body ([\\w\\W]+)$")
    @Then("^endpoint ([^\\s]+) should receive ([^\\s]+) body ([\\w\\W]+)$")
    public void receiveBody(final String endpoint, final String type, final String body) {
        runner.when(receive()
                .endpoint(endpoint)
                .message()
                .type(type)
                .body(body));
    }

    @When("^endpoint ([^\\s]+) receives body ([\\w\\W]+)$")
    @Then("^endpoint ([^\\s]+) should receive body ([\\w\\W]+)$")
    public void receiveDefault(String endpoint, String body) {
        receiveBody(endpoint, CitrusSettings.DEFAULT_MESSAGE_TYPE, body);
    }

    @When("^endpoint ([^\\s]+) receives body$")
    @Then("^endpoint ([^\\s]+) should receive body$")
    public void receiveMultilineBody(String endpoint, String body) {
        receiveBody(endpoint, CitrusSettings.DEFAULT_MESSAGE_TYPE, body);
    }

    @When("^endpoint ([^\\s]+) receives ([^\\s]+) body$")
    @Then("^endpoint ([^\\s]+) should receive ([^\\s]+) body$")
    public void shouldReceiveMultiline(String endpoint, String type, String body) {
        receiveBody(endpoint, type, body);
    }

    @And("^\\$([^\\s]+) header ([^\\s]+)(?: is |=)\"([^\"]*)\"$")
    public void addHeader(String messageId, String name, String value) {
        messages.get(messageId).setHeader(name, value);
    }

    @And("^\\$([^\\s]+) has body ([\\w\\W]+)$")
    public void addBody(String messageId, String body) {
        messages.get(messageId).setPayload(body);
    }

    @And("^\\$([^\\s]+) has body$")
    public void addBodyMultiline(String messageId, String body) {
        addBody(messageId, body);
    }
}
