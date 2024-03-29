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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.citrusframework.Citrus;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusFramework;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import org.citrusframework.yaks.YaksSettings;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import static org.citrusframework.container.FinallySequence.Builder.doFinally;
import static java.time.temporal.ChronoUnit.SECONDS;

public class KafkaSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    private String kafkaVersion = KafkaSettings.getVersion();

    private KafkaContainer kafkaContainer;

    private int startupTimeout = KafkaSettings.getStartupTimeout();

    private String serviceName = KafkaSettings.getServiceName();

    private Map<String, String> env = new HashMap<>();

    @Before
    public void before(Scenario scenario) {
        if (kafkaContainer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(KafkaContainer.class)) {
            kafkaContainer = citrus.getCitrusContext().getReferenceResolver().resolve("kafkaContainer", KafkaContainer.class);
            setConnectionSettings(kafkaContainer, context);
        }
    }

    @Given("^Kafka container version (^\\s+)$")
    public void setKafkaVersion(String version) {
        this.kafkaVersion = version;
    }

    @Given("^Kafka service name (^\\s+)$")
    public void setKafkaServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Given("^Kafka container startup timeout is (\\d+)(?: s| seconds)$")
    public void setStartupTimeout(int timeout) {
        this.startupTimeout = timeout;
    }

    @Given("^Kafka container env settings$")
    public void setEnvSettings(DataTable settings) {
        this.env.putAll(settings.asMap());
    }

    @Given("^start Kafka container$")
    public void startKafka() {
        kafkaContainer = new KafkaContainer(DockerImageName.parse(KafkaSettings.getImageName()).withTag(kafkaVersion))
                .withLabel("app", "yaks")
                .withLabel("com.joyrex2001.kubedock.name-prefix", serviceName)
                .withLabel("app.kubernetes.io/name", "kafka")
                .withLabel("app.kubernetes.io/part-of", TestContainersSettings.getTestName())
                .withLabel("app.openshift.io/connects-to", TestContainersSettings.getTestId())
                .withNetwork(Network.newNetwork())
                .withNetworkAliases(serviceName)
                .withEnv(env)
                .withStartupTimeout(Duration.of(startupTimeout, SECONDS));

        kafkaContainer.start();

        citrus.getCitrusContext().bind("kafkaContainer", kafkaContainer);

        setConnectionSettings(kafkaContainer, context);

        if (TestContainersSteps.autoRemoveResources) {
            runner.run(doFinally()
                    .actions(context -> kafkaContainer.stop()));
        }
    }

    @Given("^stop Kafka container$")
    public void stopKafka() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
        }

        env = new HashMap<>();
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param kafkaContainer
     * @param context
     */
    private void setConnectionSettings(KafkaContainer kafkaContainer, TestContext context) {
        if (!kafkaContainer.isRunning()) {
            return;
        }

        String containerId = kafkaContainer.getContainerId().substring(0, 12);
        String containerName = kafkaContainer.getContainerName();

        if (containerName.startsWith("/")) {
            containerName = containerName.substring(1);
        }

        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_HOST", kafkaContainer.getHost());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_CONTAINER_IP", kafkaContainer.getHost());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_CONTAINER_ID", containerId);
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_CONTAINER_NAME", containerName);
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_SERVICE_PORT", String.valueOf(kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT)));
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_PORT", String.valueOf(kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT)));
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_LOCAL_BOOTSTRAP_SERVERS", kafkaContainer.getBootstrapServers());

        if (YaksSettings.isLocal() || !TestContainersSettings.isKubedockEnabled()) {
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_SERVICE_NAME", serviceName);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_BOOTSTRAP_SERVERS", kafkaContainer.getBootstrapServers());
        } else {
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_SERVICE_NAME", serviceName);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_BOOTSTRAP_SERVERS", String.format("%s:%s", serviceName, kafkaContainer.getMappedPort(KafkaContainer.KAFKA_PORT)));
        }

        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "KAFKA_KUBE_DOCK_HOST", serviceName);
    }
}
