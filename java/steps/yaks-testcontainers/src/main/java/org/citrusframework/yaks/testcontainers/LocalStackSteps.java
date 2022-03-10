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

package org.citrusframework.yaks.testcontainers;

import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static java.time.temporal.ChronoUnit.SECONDS;

public class LocalStackSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    private String localStackVersion = LocalStackSettings.getVersion();
    private int startupTimeout = LocalStackSettings.getStartupTimeout();

    private LocalStackContainer localStackContainer;
    private Set<LocalStackContainer.Service> services;

    @Before
    public void before(Scenario scenario) {
        if (localStackContainer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(LocalStackContainer.class)) {
            localStackContainer = citrus.getCitrusContext().getReferenceResolver().resolve("localStackContainer", LocalStackContainer.class);
            services = citrus.getCitrusContext().getReferenceResolver().resolve("localStackEnabledServices", Set.class);
            exposeConnectionSettings(localStackContainer, context);
        } else {
            services = new HashSet<>();
        }
    }

    @Given("^LocalStack version (^\\s+)$")
    public void setLocalStackVersion(String version) {
        this.localStackVersion = version;
    }

    @Given("^LocalStack startup timeout is (\\d+)(?: s| seconds)$")
    public void setStartupTimeout(int timeout) {
        this.startupTimeout = timeout;
    }

    @Given("^Enable service (S3|KINESIS|SQS|SNS|DYNAMODB|DYNAMODB_STREAMS|IAM|API_GATEWAY|FIREHOSE|LAMBDA)$")
    public void enableService(String service) {
        services.add(LocalStackContainer.Service.valueOf(service));
    }

    @Given("^start LocalStack container$")
    public void startLocalStack() {
        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack").withTag(localStackVersion))
                .withServices(services.toArray(LocalStackContainer.Service[]::new))
                .waitingFor(Wait.forListeningPort()
                        .withStartupTimeout(Duration.of(startupTimeout, SECONDS)));

        localStackContainer.start();

        citrus.getCitrusContext().bind("localStackContainer", localStackContainer);
        citrus.getCitrusContext().bind("localStackEnabledServices", services);

        exposeConnectionSettings(localStackContainer, context);

        if (TestContainersSteps.autoRemoveResources) {
            runner.run(doFinally()
                    .actions(context -> localStackContainer.stop()));
        }
    }

    @Given("^stop LocalStack container$")
    public void stopLocalStack() {
        if (localStackContainer != null) {
            localStackContainer.stop();
        }
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param localStackContainer
     * @param context
     */
    private void exposeConnectionSettings(LocalStackContainer localStackContainer, TestContext context) {
        if (localStackContainer.isRunning()) {
            String containerId = localStackContainer.getContainerId().substring(0, 12);

            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_CONTAINER_IP", localStackContainer.getContainerIpAddress());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_CONTAINER_ID", containerId);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_CONTAINER_NAME", localStackContainer.getContainerName());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_SERVICE_NAME", "kd-" + containerId);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_SERVICE_PORT", String.valueOf(localStackContainer.getMappedPort(4566)));
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_REGION", localStackContainer.getRegion());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_ACCESS_KEY", localStackContainer.getAccessKey());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "LOCALSTACK_SECRET_KEY", localStackContainer.getSecretKey());

            services.forEach(service -> {
                context.setVariable(String.format("%sLOCALSTACK_%s_URL", TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX, service.getName().toUpperCase(Locale.US)), String.format("http://kd-%s:%s", containerId, localStackContainer.getEndpointOverride(service).getPort()));
                context.setVariable(String.format("%sLOCALSTACK_%s_LOCAL_URL", TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX, service.getName().toUpperCase(Locale.US)), localStackContainer.getEndpointOverride(service).toString());
                context.setVariable(String.format("%sLOCALSTACK_%s_PORT", TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX, service.getName().toUpperCase(Locale.US)), localStackContainer.getEndpointOverride(service).getPort());
            });
        }
    }
}
