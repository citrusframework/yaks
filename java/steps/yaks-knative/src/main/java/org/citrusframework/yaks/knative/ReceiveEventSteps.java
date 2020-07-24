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

package org.citrusframework.yaks.knative;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.message.MessageType;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.knative.ce.CloudEventSupport;
import org.springframework.http.HttpStatus;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static org.citrusframework.yaks.knative.actions.KnativeActionBuilder.knative;

/**
 * @author Christoph Deppisch
 */
public class ReceiveEventSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private HttpServer httpServer;

    private int servicePort = KnativeSettings.getServicePort();
    private String serviceName = KnativeSettings.getServiceName();

    private String eventData;

    private long timeout = KnativeSettings.getEventConsumerTimeout();

    private KubernetesClient k8sClient;

    @Before
    public void before(Scenario scenario) {
        if (httpServer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(serviceName)) {
            httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(serviceName, HttpServer.class);
            servicePort = httpServer.getPort();
            timeout = httpServer.getDefaultTimeout();
        } else {
            httpServer = new HttpServerBuilder()
                    .port(servicePort)
                    .defaultStatus(HttpStatus.ACCEPTED)
                    .timeout(timeout)
                    .name(serviceName)
                    .build();

            citrus.getCitrusContext().getReferenceResolver().bind(serviceName, httpServer);
            try {
                httpServer.afterPropertiesSet();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize Http server as Knative service", e);
            }
        }

        if (k8sClient == null) {
            k8sClient = KnativeSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^Knative service \"([^\"\\s]+)\"$")
    public void setServiceName(String name) {
        this.serviceName = name;
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(name, HttpServer.class);
        }

        httpServer.setName(serviceName);
    }

    @Given("^Knative service port (\\d+)$")
    public void setServicePort(int port) {
        this.servicePort = port;
        httpServer.setPort(port);
    }

    @Given("^Knative event consumer timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Given("^(?:expect|verify) Knative event data$")
    public void setEventDataMultiline(String data) {
        setEventData(data);
    }

    @Given("^(?:expect|verify) Knative event data: (.+)$")
    public void setEventData(String data) {
        this.eventData = data;
    }

    @Then("^(?:receive|verify) Knative event$")
    public void receiveEvent(DataTable attributes) {
        receiveEvent(CloudEventSupport.createEventRequest(eventData, attributes.asMap(String.class, String.class)));
    }

    @Then("^(?:receive|verify) Knative event as json$")
    public void receiveEventJson(String json) {
        receiveEvent(CloudEventSupport.createEventRequest(eventData, CloudEventSupport.attributesFromJson(json)));
    }

    @Given("^create Knative event consumer service ([^\\s]+)$")
    public void createService(String serviceName) {
        createService(serviceName, servicePort);
    }

    @Given("^create Knative event consumer service ([^\\s]+) with target port (\\d+)$")
    public void createService(String serviceName, int targetPort) {
        setServicePort(targetPort);
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        runner.given(knative().client(k8sClient).createService(serviceName).targetPort(targetPort));

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient).deleteService(serviceName)));
        }
    }

    /**
     * Receives cloud event as Http request.
     * @param request
     */
    private void receiveEvent(HttpMessage request) {
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        runner.run(http().server(httpServer)
                .receive()
                .post()
                .timeout(timeout)
                .messageType(MessageType.JSON)
                .message(request));

        runner.run(http().server(httpServer)
                .send()
                .response(HttpStatus.ACCEPTED));
    }
}
