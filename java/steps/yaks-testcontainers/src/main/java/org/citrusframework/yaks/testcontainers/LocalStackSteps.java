/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.testcontainers;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import org.citrusframework.Citrus;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusFramework;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSettings;
import org.citrusframework.yaks.kubernetes.KubernetesVariableNames;
import org.citrusframework.yaks.testcontainers.aws2.AWS2Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.citrusframework.container.FinallySequence.Builder.doFinally;

public class LocalStackSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    private String localStackVersion = LocalStackSettings.getVersion();
    private int startupTimeout = LocalStackSettings.getStartupTimeout();

    private AWS2Container aws2Container;
    private final Set<AWS2Container.AWS2Service> services = new HashSet<>();

    private Map<String, String> env = new HashMap<>();

    private String serviceName = LocalStackSettings.getServiceName();

    @Before
    public void before(Scenario scenario) {
        if (aws2Container == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(AWS2Container.class)) {
            aws2Container = citrus.getCitrusContext().getReferenceResolver().resolve("aws2Container", AWS2Container.class);
            services.addAll(Arrays.asList(aws2Container.getServices()));
            exposeConnectionSettings(aws2Container, context);
        }
    }

    @Given("^LocalStack version (^\\s+)$")
    public void setLocalStackVersion(String version) {
        this.localStackVersion = version;
    }

    @Given("^LocalStack service name (^\\s+)$")
    public void setLocalStackServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Given("^LocalStack env settings$")
    public void setEnvSettings(DataTable settings) {
        this.env.putAll(settings.asMap());
    }

    @Given("^LocalStack startup timeout is (\\d+)(?: s| seconds)$")
    public void setStartupTimeout(int timeout) {
        this.startupTimeout = timeout;
    }

    @Given("^Enable service (S3|KINESIS|SQS|SNS|DYNAMODB|DYNAMODB_STREAMS|IAM|API_GATEWAY|FIREHOSE|LAMBDA)$")
    public void enableService(String service) {
        services.add(AWS2Container.AWS2Service.valueOf(service));
    }

    @Given("^start LocalStack container$")
    public void startLocalStack() {
        aws2Container = new AWS2Container(localStackVersion)
                .withServices(services.toArray(AWS2Container.AWS2Service[]::new))
                .withLabel("app", "yaks")
                .withLabel("com.joyrex2001.kubedock.name-prefix", serviceName)
                .withLabel("app.kubernetes.io/name", "build")
                .withLabel("app.kubernetes.io/part-of", TestContainersSettings.getTestName())
                .withLabel("app.openshift.io/connects-to", TestContainersSettings.getTestId())
                .withNetwork(Network.newNetwork())
                .withNetworkAliases(serviceName)
                .withEnv(env)
                .waitingFor(Wait.forListeningPort()
                        .withStartupTimeout(Duration.of(startupTimeout, SECONDS)));

        aws2Container.start();

        citrus.getCitrusContext().bind("aws2Container", aws2Container);

        exposeConnectionSettings(aws2Container, context);

        if (TestContainersSteps.autoRemoveResources) {
            runner.run(doFinally()
                    .actions(context -> aws2Container.stop()));
        }
    }

    @Given("^stop LocalStack container$")
    public void stopLocalStack() {
        if (aws2Container != null) {
            aws2Container.stop();
        }

        env = new HashMap<>();
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param aws2Container
     * @param context
     */
    private void exposeConnectionSettings(AWS2Container aws2Container, TestContext context) {
        if (aws2Container.isRunning()) {
            URI serviceEndpoint = aws2Container.getServiceEndpoint();

            String containerId = aws2Container.getContainerId().substring(0, 12);
            String containerName = aws2Container.getContainerName();

            if (containerName.startsWith("/")) {
                containerName = containerName.substring(1);
            }

            context.setVariable(getEnvVarName("HOST"), aws2Container.getHost());
            context.setVariable(getEnvVarName("CONTAINER_IP"), aws2Container.getHost());
            context.setVariable(getEnvVarName("CONTAINER_ID"), containerId);
            context.setVariable(getEnvVarName("CONTAINER_NAME"), containerName);
            context.setVariable(getEnvVarName("REGION"), aws2Container.getRegion());
            context.setVariable(getEnvVarName("ACCESS_KEY"), aws2Container.getAccessKey());
            context.setVariable(getEnvVarName("SECRET_KEY"), aws2Container.getSecretKey());
            context.setVariable(getEnvVarName("SERVICE_PORT"), serviceEndpoint.getPort());
            context.setVariable(getEnvVarName("SERVICE_LOCAL_URL"), String.format("http://localhost:%s", serviceEndpoint.getPort()));

            if (YaksSettings.isLocal() || !TestContainersSettings.isKubedockEnabled()) {
                context.setVariable(getEnvVarName("SERVICE_NAME"), serviceName);
                context.setVariable(getEnvVarName("SERVICE_URL"), String.format("http://localhost:%s", serviceEndpoint.getPort()));
            } else {
                context.setVariable(getEnvVarName("SERVICE_NAME"), serviceName);
                context.setVariable(getEnvVarName("SERVICE_URL"), String.format("http://%s:%s", serviceName, serviceEndpoint.getPort()));
            }

            services.forEach(service -> {
                String aws2ServiceName = service.getServiceName().toUpperCase(Locale.US);

                if (YaksSettings.isLocal() || !TestContainersSettings.isKubedockEnabled()) {
                    context.setVariable(getEnvVarName(String.format("%s_URL", aws2ServiceName)), String.format("http://localhost:%s", serviceEndpoint.getPort()));
                } else {
                    context.setVariable(getEnvVarName(String.format("%s_URL", aws2ServiceName)), String.format("http://%s:%s", serviceName, serviceEndpoint.getPort()));
                }

                context.setVariable(getEnvVarName(String.format("%s_LOCAL_URL", aws2ServiceName)), String.format("http://localhost:%s", serviceEndpoint.getPort()));
                context.setVariable(getEnvVarName(String.format("%s_PORT", aws2ServiceName)), serviceEndpoint.getPort());
            });

            context.setVariable(getEnvVarName("KUBE_DOCK_SERVICE_URL"), String.format("http://%s:%s", serviceName, serviceEndpoint.getPort()));
            context.setVariable(getEnvVarName("KUBE_DOCK_HOST"), serviceName);

            for (Map.Entry<Object, Object> connectionProperty : aws2Container.getConnectionProperties().entrySet()) {
                context.setVariable(connectionProperty.getKey().toString(), connectionProperty.getValue().toString());
            }
        }
    }

    private String getEnvVarName(String variable) {
        return String.format("%sLOCALSTACK_%s", TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX, variable);
    }

    private String getNamespace(TestContext context) {
        if (context.getVariables().containsKey(KubernetesVariableNames.NAMESPACE.value())) {
            return context.getVariable(KubernetesVariableNames.NAMESPACE.value());
        }

        return KubernetesSettings.getNamespace();
    }
}
