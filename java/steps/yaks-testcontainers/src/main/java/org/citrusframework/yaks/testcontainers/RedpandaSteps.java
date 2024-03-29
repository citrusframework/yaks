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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.citrusframework.container.FinallySequence.Builder.doFinally;
import static java.time.temporal.ChronoUnit.SECONDS;

public class RedpandaSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    public static final int REDPANDA_PORT = 9092;

    private String redpandaVersion = RedpandaSettings.getVersion();

    private RedpandaContainer redpandaContainer;

    private int startupTimeout = RedpandaSettings.getStartupTimeout();

    private Map<String, String> env = new HashMap<>();

    private String serviceName = RedpandaSettings.getServiceName();

    @Before
    public void before(Scenario scenario) {
        if (redpandaContainer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(RedpandaContainer.class)) {
            redpandaContainer = citrus.getCitrusContext().getReferenceResolver().resolve("redpandaContainer", RedpandaContainer.class);
            setConnectionSettings(redpandaContainer, context);
        }
    }

    @Given("^Redpanda version (^\\s+)$")
    public void setRedpandaVersion(String version) {
        this.redpandaVersion = version;
    }

    @Given("^Redpanda service name (^\\s+)$")
    public void setRedpandaServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Given("^Redpanda startup timeout is (\\d+)(?: s| seconds)$")
    public void setStartupTimeout(int timeout) {
        this.startupTimeout = timeout;
    }

    @Given("^Redpanda env settings$")
    public void setEnvSettings(DataTable settings) {
        this.env.putAll(settings.asMap());
    }

    @Given("^start Redpanda container$")
    public void startRedpanda() {
        redpandaContainer = new RedpandaContainer(DockerImageName.parse(RedpandaSettings.getImageName()).withTag(redpandaVersion))
                .withLabel("app", "yaks")
                .withLabel("com.joyrex2001.kubedock.name-prefix", serviceName)
                .withLabel("app.kubernetes.io/name", "redpanda")
                .withLabel("app.kubernetes.io/part-of", TestContainersSettings.getTestName())
                .withLabel("app.openshift.io/connects-to", TestContainersSettings.getTestId())
                .withNetwork(Network.newNetwork())
                .withNetworkAliases(serviceName)
                .withEnv(env)
                .waitingFor(Wait.forLogMessage(".*Successfully started Redpanda!.*", 1)
                        .withStartupTimeout(Duration.of(startupTimeout, SECONDS)))
                // TODO: Remove once Redpanda container works with Podman
                . withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint();
                    cmd.withEntrypoint("/entrypoint-tc.sh");
                    cmd.withUser("root:root");
                })
                .withCommand("redpanda", "start", "--mode=dev-container", "--smp=1", "--memory=1G");

        redpandaContainer.start();

        citrus.getCitrusContext().bind("redpandaContainer", redpandaContainer);

        setConnectionSettings(redpandaContainer, context);

        if (TestContainersSteps.autoRemoveResources) {
            runner.run(doFinally()
                    .actions(context -> redpandaContainer.stop()));
        }
    }

    @Given("^stop Redpanda container$")
    public void stopRedpanda() {
        if (redpandaContainer != null) {
            redpandaContainer.stop();
        }

        env = new HashMap<>();
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param redpandaContainer
     * @param context
     */
    private void setConnectionSettings(RedpandaContainer redpandaContainer, TestContext context) {
        if (!redpandaContainer.isRunning()) {
            return;
        }

        String containerId = redpandaContainer.getContainerId().substring(0, 12);
        String containerName = redpandaContainer.getContainerName();

        if (containerName.startsWith("/")) {
            containerName = containerName.substring(1);
        }

        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_HOST", redpandaContainer.getHost());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_CONTAINER_IP", redpandaContainer.getHost());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_CONTAINER_ID", containerId);
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_CONTAINER_NAME", containerName);
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_SERVICE_PORT", String.valueOf(redpandaContainer.getMappedPort(REDPANDA_PORT)));
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_PORT", String.valueOf(redpandaContainer.getMappedPort(REDPANDA_PORT)));
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_LOCAL_BOOTSTRAP_SERVERS", redpandaContainer.getBootstrapServers());

        if (YaksSettings.isLocal() || !TestContainersSettings.isKubedockEnabled()) {
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_SERVICE_NAME", serviceName);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_BOOTSTRAP_SERVERS", redpandaContainer.getBootstrapServers());
        } else {
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_SERVICE_NAME", serviceName);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_BOOTSTRAP_SERVERS", String.format("%s:%s", serviceName, redpandaContainer.getMappedPort(REDPANDA_PORT)));
        }

        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "REDPANDA_KUBE_DOCK_HOST", serviceName);
    }
}
