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

package org.citrusframework.yaks.kubernetes;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.message.MessageType;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.http.HttpStatus;

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static org.citrusframework.yaks.kubernetes.actions.KubernetesActionBuilder.kubernetes;

public class KubernetesSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private HttpServer httpServer;

    private int servicePort = KubernetesSettings.getServicePort();
    private String serviceName = KubernetesSettings.getServiceName();

    private long timeout = KubernetesSettings.getServiceTimeout();

    private KubernetesClient k8sClient;

    private boolean autoRemoveResources = KubernetesSettings.isAutoRemoveResources();

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
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^Disable auto removal of Kubernetes resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

    @Given("^Kubernetes namespace ([^\\s]+)$")
    public void setNamespace(String namespace) {
        // update the test variable that points to the namespace
        runner.run(createVariable(KubernetesVariableNames.NAMESPACE.value(), namespace));
    }

    @Given("^Kubernetes timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Given("^Kubernetes service \"([^\"\\s]+)\"$")
    public void setServiceName(String name) {
        this.serviceName = name;
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(name, HttpServer.class);
        } else if (httpServer != null) {
            citrus.getCitrusContext().getReferenceResolver().bind(serviceName, httpServer);
            httpServer.setName(serviceName);
        }
    }

    @Given("^Kubernetes service port (\\d+)$")
    public void setServicePort(int port) {
        this.servicePort = port;
        if (httpServer != null) {
            httpServer.setPort(port);
        }
    }

    @Given("^create Kubernetes service ([^\\s]+)$")
    public void createService(String serviceName) {
        createService(serviceName, servicePort);
    }

    @Given("^create Kubernetes service ([^\\s]+) with target port (\\d+)$")
    public void createService(String serviceName, int targetPort) {
        setServiceName(serviceName);
        setServicePort(targetPort);
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        runner.given(kubernetes().client(k8sClient).createService(serviceName).targetPort(targetPort));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient).deleteService(serviceName)));
        }
    }

    public void receiveServiceRequest(HttpMessage request, MessageType messageType) {
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        runner.run(http().server(httpServer)
                .receive()
                .post()
                .timeout(timeout)
                .messageType(messageType)
                .message(request));
    }

    public void sendServiceResponse(HttpStatus status) {
        runner.run(http().server(httpServer)
                .send()
                .response(status));
    }
}
