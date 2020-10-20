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
import com.consol.citrus.annotations.CitrusAnnotations;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.message.MessageType;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.citrusframework.yaks.knative.ce.CloudEventSupport;
import org.citrusframework.yaks.kubernetes.KubernetesSteps;
import org.springframework.http.HttpStatus;

/**
 * @author Christoph Deppisch
 */
public class ReceiveEventSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    @CitrusFramework
    private Citrus citrus;

    private String eventData;

    private KubernetesSteps kubernetesSteps;

    @Before
    public void before(Scenario scenario) {
        kubernetesSteps = new KubernetesSteps();
        CitrusAnnotations.injectAll(kubernetesSteps, citrus, context);
        kubernetesSteps.before(scenario);
        kubernetesSteps.configureTimeout(KnativeSettings.getEventConsumerTimeout());
        kubernetesSteps.setServiceName(KnativeSettings.getServiceName());
        kubernetesSteps.setServicePort(KnativeSettings.getServicePort());
    }

    @Given("^Knative service \"([^\"\\s]+)\"$")
    public void setServiceName(String name) {
        kubernetesSteps.setServiceName(name);
    }

    @Given("^Knative service port (\\d+)$")
    public void setServicePort(int port) {
        kubernetesSteps.setServicePort(port);
    }

    @Given("^Knative event consumer timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        kubernetesSteps.configureTimeout(timeout);
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
        kubernetesSteps.createService(serviceName);
    }

    @Given("^create Knative event consumer service ([^\\s]+) with target port (\\d+)$")
    public void createService(String serviceName, int targetPort) {
        kubernetesSteps.createService(serviceName, targetPort);
    }

    /**
     * Receives cloud event as Http request.
     * @param request
     */
    private void receiveEvent(HttpMessage request) {
        kubernetesSteps.receiveServiceRequest(request, MessageType.JSON);
        kubernetesSteps.sendServiceResponse(HttpStatus.ACCEPTED);
    }
}
