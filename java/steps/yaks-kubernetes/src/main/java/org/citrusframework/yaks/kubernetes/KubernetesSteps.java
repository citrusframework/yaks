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
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.util.FileUtils;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.kubernetes.actions.CreateServiceAction;
import org.citrusframework.yaks.kubernetes.actions.VerifyPodAction;
import org.springframework.http.HttpStatus;

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;
import static com.consol.citrus.container.Assert.Builder.assertException;
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
    private int maxAttempts = KubernetesSettings.getMaxAttempts();
    private long delayBetweenAttempts = KubernetesSettings.getDelayBetweenAttempts();

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

    @Given("^Kubernetes resource polling configuration$")
    public void configureResourcePolling(Map<String, Object> configuration) {
        maxAttempts = Integer.parseInt(configuration.getOrDefault("maxAttempts", maxAttempts).toString());
        delayBetweenAttempts = Long.parseLong(configuration.getOrDefault("delayBetweenAttempts", delayBetweenAttempts).toString());
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
            Object component = citrus.getCitrusContext().getReferenceResolver().resolve(name);
            if (component instanceof HttpServer) {
                httpServer = (HttpServer) component;
            }
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
    public void createCustomResource(String resourceType, String yaml) {
        KubernetesResource resource = KubernetesSupport.yaml().loadAs(yaml, KubernetesResource.class);

        runner.run(kubernetes().client(k8sClient)
                .customResources()
                .create()
                .type(resourceType)
                .kind(resource.getKind())
                .apiVersion(resource.getApiVersion())
                .content(yaml));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .customResources()
                            .delete(resource.getMetadata().getName())
                            .type(resourceType)
                            .kind(resource.getKind())
                            .apiVersion(resource.getApiVersion())));
        }
    }

    @Given("^load Kubernetes custom resource ([^\\s]+) in ([^\\s]+)$")
    public void createCustomResourceFromFile(String fileName, String resourceType) {
        try {
            createCustomResource(resourceType, FileUtils.readToString(FileUtils.getFileResource(fileName)));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read custom resource from file", e);
        }
    }

    @Given("^delete Kubernetes custom resource in ([^\\s]+)$")
    public void deleteCustomResource(String resourceType, String yaml) {
        KubernetesResource resource = KubernetesSupport.yaml().loadAs(yaml, KubernetesResource.class);

        runner.run(kubernetes().client(k8sClient)
                        .customResources()
                        .delete(resource.getMetadata().getName())
                        .type(resourceType)
                        .kind(resource.getKind())
                        .apiVersion(resource.getApiVersion()));
    }

    @Given("^delete Kubernetes custom resource ([^\\s]+) in ([^\\s]+)$")
    public void deleteCustomResourceFromFile(String fileName, String resourceType) {
        try {
            deleteCustomResource(resourceType, FileUtils.readToString(FileUtils.getFileResource(fileName)));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read custom resource from file", e);
        }
    }

    @Given("^create Kubernetes resource$")
    public void createResource(String content) {
        runner.run(kubernetes().client(k8sClient)
                .resources()
                .create()
                .content(content));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .resources()
                            .delete(content)));
        }
    }

    @Given("^load Kubernetes resource ([^\\s]+)$")
    public void createResourceFromFile(String fileName) {
        try {
            createResource(FileUtils.readToString(FileUtils.getFileResource(fileName)));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read resource from file", e);
        }
    }

    @Given("^delete Kubernetes resource$")
    public void deleteResource(String yaml) {
        runner.run(kubernetes().client(k8sClient)
                .resources()
                .delete(yaml));
    }

    @Given("^delete Kubernetes resource ([^\\s]+)$")
    public void deleteResourceFromFile(String fileName) {
        try {
            deleteResource(FileUtils.readToString(FileUtils.getFileResource(fileName)));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read resource from file", e);
        }
    }

    @Given("^create Kubernetes secret ([^\\s]+)$")
    public void createSecret(String name, Map<String, String> properties) {
        runner.run(kubernetes().client(k8sClient)
                .secrets()
                .create(name)
                .properties(properties));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .secrets()
                            .delete(name)));
        }
    }

    @Given("^load Kubernetes secret from file ([^\\s]+).properties$")
    public void createSecret(String fileName) {
        runner.run(kubernetes().client(k8sClient)
                .secrets()
                .create(fileName)
                .fromFile(fileName + ".properties"));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .secrets()
                            .delete(fileName)));
        }
    }

    @Given("^create Kubernetes service$")
    public void createService() {
        createService(serviceName, servicePort);
    }

    @Given("^create Kubernetes service ([^\\s]+)$")
    public void createService(String serviceName) {
        createService(serviceName, servicePort);
    }

    @Given("^create Kubernetes service ([^\\s]+) with target port (\\d+)$")
    public void createService(String serviceName, int targetPort) {
        createService(serviceName, 80, targetPort);
    }

    @Given("^create Kubernetes service ([^\\s]+) with port mapping (\\d+):(\\d+)$")
    public void createService(String serviceName, int port, int targetPort) {
        initializeService(serviceName, targetPort);

        runner.given(kubernetes().client(k8sClient)
                .services()
                .create(serviceName)
                .port(port)
                .targetPort(targetPort));

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .services()
                            .delete(serviceName)));
        }
    }

    @Given("^create Kubernetes service ([^\\s]+) with port mappings$")
    public void createServiceWithPortMappings(String serviceName, DataTable portMappings) {
        CreateServiceAction.Builder createServiceAction = kubernetes().client(k8sClient)
                .services()
                .create(serviceName);

        Map<Integer, Integer> mappings = portMappings.asMap(Integer.class, Integer.class);

        initializeService(serviceName, mappings.values().iterator().next());

        mappings.forEach(createServiceAction::portMapping);

        runner.run(createServiceAction);

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(kubernetes().client(k8sClient)
                            .services()
                            .delete(serviceName)));
        }
    }

    @Given("^delete Kubernetes service ([^\\s]+)$")
    public void deleteService(String serviceName) {
        runner.run(kubernetes()
                .client(k8sClient)
                .services()
                .delete(serviceName));
    }

    @Given("^delete Kubernetes secret ([^\\s]+)$")
    public void deleteSecret(String secretName) {
        runner.run(kubernetes().client(k8sClient)
                .secrets()
                .delete(secretName));
    }

    @Given("^wait for condition=([^\\s]+) on Kubernetes custom resource ([^\\s]+) in ([^\\s]+)$")
    public void resourceShouldMatchCondition(String condition, String name, String resourceType) {
        runner.run(kubernetes().client(k8sClient)
                .customResources()
                .verify(name)
                .type(resourceType)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .condition(condition));
    }

    @Given("^Kubernetes custom resource ([^\\s]+) in ([^\\s]+) is ready")
    @Then("^Kubernetes custom resource ([^\\s]+) in ([^\\s]+) should be ready")
    public void resourceShouldBeReady(String name, String resourceType) {
        resourceShouldMatchCondition("Ready", name, resourceType);
    }

    @Given("^wait for condition=([^\\s]+) on Kubernetes custom resource in ([^\\s]+) labeled with ([^\\s]+)=([^\\s]+)$")
    public void resourceLabeledShouldMatchCondition(String condition, String resourceType, String label, String value) {
        runner.run(kubernetes().client(k8sClient)
                .customResources()
                .verify(label, value)
                .type(resourceType)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .condition(condition));
    }

    @Given("^Kubernetes custom resource in ([^\\s]+) labeled with ([^\\s]+)=([^\\s]+) is ready")
    @Then("^Kubernetes custom resource in ([^\\s]+) labeled with ([^\\s]+)=([^\\s]+) should be ready")
    public void resourceLabeledShouldBeReady(String resourceType, String label, String value) {
        resourceLabeledShouldMatchCondition("Ready", resourceType, label, value);
    }

    @Given("^wait for Kubernetes pod ([a-z0-9-]+)$")
    public void waitForRunningPod(String name) {
        podShouldBeInPhase(name, "running");
    }

    @Given("^Kubernetes pod ([a-z0-9-]+) is (running|stopped)$")
    @Then("^Kubernetes pod ([a-z0-9-]+) should be (running|stopped)$")
    public void podShouldBeInPhase(String name, String status) {
        VerifyPodAction.Builder action = kubernetes().client(k8sClient)
                .pods()
                .verify(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts);

        if (status.equals("running")) {
            action.isRunning();
        } else {
            action.isStopped();
        }

        runner.run(action);
    }

    @Given("^wait for Kubernetes pod labeled with ([^\\s]+)=([^\\s]+)$")
    public void waitForRunningPod(String label, String value) {
        podByLabelShouldBeInPhase(label, value, "running");
    }

    @Given("^Kubernetes pod labeled with ([^\\s]+)=([^\\s]+) is (running|stopped)$")
    @Then("^Kubernetes pod labeled with ([^\\s]+)=([^\\s]+) should be (running|stopped)$")
    public void podByLabelShouldBeInPhase(String label, String value, String status) {
        VerifyPodAction.Builder action = kubernetes().client(k8sClient)
                .pods()
                .verify(label, value)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts);

        if (status.equals("running")) {
            action.isRunning();
        } else {
            action.isStopped();
        }

        runner.run(action);
    }

    @Then("^Kubernetes pod ([a-z0-9-]+) should print (.*)$")
    public void podShouldPrint(String name, String message) {
        runner.run(kubernetes().client(k8sClient)
                .pods()
                .verify(name)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .waitForLogMessage(message));
    }

    @Then("^Kubernetes pod ([a-z0-9-]+) should not print (.*)$")
    public void podShouldNotPrint(String name, String message) {
        runner.run(assertException()
                .exception(ActionTimeoutException.class)
                .when(kubernetes().client(k8sClient)
                        .pods()
                        .verify(name)
                        .maxAttempts(maxAttempts)
                        .delayBetweenAttempts(delayBetweenAttempts)
                        .waitForLogMessage(message)));
    }

    @Then("^Kubernetes pod labeled with ([^\\s]+)=([^\\s]+) should print (.*)$")
    public void podByLabelShouldPrint(String label, String value, String message) {
        runner.run(kubernetes().client(k8sClient)
                .pods()
                .verify(label, value)
                .maxAttempts(maxAttempts)
                .delayBetweenAttempts(delayBetweenAttempts)
                .waitForLogMessage(message));
    }

    @Then("^Kubernetes pod labeled with ([^\\s]+)=([^\\s]+) should not print (.*)$")
    public void podByLabelShouldNotPrint(String label, String value, String message) {
        runner.run(assertException()
                .exception(ActionTimeoutException.class)
                .when(kubernetes().client(k8sClient)
                        .pods()
                        .verify(label, value)
                        .maxAttempts(maxAttempts)
                        .delayBetweenAttempts(delayBetweenAttempts)
                        .waitForLogMessage(message)));
    }

    public void receiveServiceRequest(HttpMessage request, MessageType messageType) {
        initializeService(serviceName, servicePort);

        runner.run(http().server(httpServer)
                .receive()
                .post()
                .timeout(timeout)
                .message(request.setType(messageType)));
    }

    public void sendServiceResponse(HttpStatus status) {
        runner.run(http().server(httpServer)
                .send()
                .response(status));
    }

    private void initializeService(String serviceName, int targetPort) {
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(serviceName) &&
                citrus.getCitrusContext().getReferenceResolver().resolve(serviceName) instanceof HttpServer) {
            setServiceName(serviceName);
            setServicePort(targetPort);

            if (!httpServer.isRunning()) {
                httpServer.start();
            }
        }
    }
}
