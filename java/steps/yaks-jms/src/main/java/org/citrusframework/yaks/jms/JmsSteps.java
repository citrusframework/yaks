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

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

import org.springframework.util.Assert;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.jms.message.JmsMessage;

import javax.jms.ConnectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class JmsSteps {

    private static final long TIMEOUT = System.getenv("YAKS_JMS_TIMEOUT") != null ? Integer.valueOf(System.getenv("YAKS_JMS_TIMEOUT")) : TimeUnit.SECONDS.toMillis(60);

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private JmsEndpoint jmsEndpoint;

    private ConnectionFactory connectionFactory;

    @Given("^(?:JMS|jms) connection factory (.+)$")
    public void setConnection(String className, DataTable properties) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Assert.notNull(className, "Connection factory not specified");
        Class connectionFactoryClass = Class.forName(className);

        List<String> constructorArgs = properties.asList();
        Constructor constructor = Arrays.stream(connectionFactoryClass.getConstructors())
                .filter(c -> c.getParameterCount() == constructorArgs.size()
                        && Arrays.stream(c.getParameterTypes()).allMatch(type -> type.equals(String.class)))
        .findFirst().orElseThrow(() -> new IllegalStateException("Can't find proper constructor"));

        Object instance = constructor.newInstance(constructorArgs.toArray(new String[constructorArgs.size()]));
        if(!(instance instanceof ConnectionFactory)) {
            throw new IllegalStateException("Class is not instance of " + ConnectionFactory.class.getName());
        }

        connectionFactory = (ConnectionFactory) instance;
    }

    @Given("^(?:JMS|jms) destination: (.+)$")
    public void jmsEndpoint(String destination) {
        jmsEndpoint = CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(destination)
                .build();
    }

    @When("^send message to JMS broker with body: (.+)")
    @Given("^message in JMS broker with body: (.+)$")
    public void sendToBroker(String body) {
        runner.run(send().endpoint(jmsEndpoint).message(new JmsMessage(body)));
    }

    @Then("^expect message in JMS broker with body: (.+)$")
    public void receiveFromBroker(String body) {
        runner.run(receive().endpoint(jmsEndpoint).timeout(TIMEOUT).message(new JmsMessage(body)));
    }

}
