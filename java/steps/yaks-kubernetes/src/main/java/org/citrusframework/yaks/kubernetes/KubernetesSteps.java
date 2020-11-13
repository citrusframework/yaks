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

import java.io.IOException;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.util.FileUtils;
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
            httpServer.initialize();
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

    @Given("^create Kubernetes custom resource in ([^\\s]+)$")
    public void createCustomResource(String resourceType, String content) {
        Map<String, Object> yamlContent = KubernetesSupport.yaml().load(content);
        Object metadata = yamlContent.get("metadata");

        if (!(metadata instanceof Map)) {
            throw new CitrusRuntimeException("Missing metadata on Kubernetes custom resource");
        }

        String name = ((Map<String, Object>) metadata).getOrDefault("name", "").toString();
        String kind = yamlContent.getOrDefault("kind", "").toString();
        String apiVersion = yamlContent.getOrDefault("apiVersion", KubernetesSupport.kubernetesApiVersion()).toString();

        runner.run(kubernetes().client(k8sClient).createCustomResource()
                .type(resourceType)
                .kind(kind)
                .apiVersion(apiVersion)
                .content(content));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .deleteCustomResource(name)
                            .type(resourceType)
                            .kind(kind)
                            .apiVersion(apiVersion)));
        }
    }

    @Given("^load Kubernetes custom resource ([^\\s]+) in ([^\\s]+)$")
    public void createCustomResourceFromFile(String fileName, String resourceType) {
        try {
            createCustomResource(resourceType, FileUtils.readToString(FileUtils.getFileResource(fileName)));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read properties file", e);
        }
    }

    @Given("^create Kubernetes secret ([^\\s]+)$")
    public void createSecret(String name, Map<String, String> properties) {
        runner.run(kubernetes().client(k8sClient).createSecret(name).properties(properties));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient).deleteSecret(name)));
        }
    }

    @Given("^load Kubernetes secret from file ([^\\s]+).properties$")
    public void createSecret(String fileName) {
        runner.run(kubernetes().client(k8sClient).createSecret(fileName).fromFile(fileName + ".properties"));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient).deleteSecret(fileName)));
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

    @Given("^delete Kubernetes service ([^\\s]+)$")
    public void deleteService(String serviceName) {
        runner.run(kubernetes()
                .client(k8sClient)
                .deleteService(serviceName));
    }

    @Given("^delete Kubernetes secret ([^\\s]+)$")
    public void deleteSecret(String secretName) {
        runner.run(kubernetes()
                .client(k8sClient)
                .deleteSecret(secretName));
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
