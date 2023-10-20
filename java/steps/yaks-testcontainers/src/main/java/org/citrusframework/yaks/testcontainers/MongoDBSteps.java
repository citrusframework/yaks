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
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static org.citrusframework.container.FinallySequence.Builder.doFinally;
import static java.time.temporal.ChronoUnit.SECONDS;

public class MongoDBSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    private String mongoDBVersion = MongoDBSettings.getMongoDBVersion();
    private int startupTimeout = MongoDBSettings.getStartupTimeout();

    private MongoDBContainer mongoDBContainer;

    private Map<String, String> env = new HashMap<>();

    @Before
    public void before(Scenario scenario) {
        if (mongoDBContainer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(MongoDBContainer.class)) {
            mongoDBContainer = citrus.getCitrusContext().getReferenceResolver().resolve("mongoDBContainer", MongoDBContainer.class);
            setConnectionSettings(mongoDBContainer, context);
        }
    }

    @Given("^MongoDB version (^\\s+)$")
    public void setMongoDBVersion(String version) {
        this.mongoDBVersion = version;
    }

    @Given("^MongoDB startup timeout is (\\d+)(?: s| seconds)$")
    public void setStartupTimeout(int timeout) {
        this.startupTimeout = timeout;
    }

    @Given("^MongoDB env settings$")
    public void setEnvSettings(DataTable settings) {
        this.env.putAll(settings.asMap());
    }

    @Given("^start MongoDB container$")
    public void startMongo() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo").withTag(mongoDBVersion))
                .withLabel("app", "yaks")
                .withLabel("app.kubernetes.io/name", "mongodb")
                .withLabel("app.kubernetes.io/part-of", TestContainersSettings.getTestName())
                .withLabel("app.openshift.io/connects-to", TestContainersSettings.getTestId())
                .withNetworkAliases("mongodb")
                .withEnv(env)
                .waitingFor(Wait.forLogMessage("(?i).*waiting for connections.*", 1)
                        .withStartupTimeout(Duration.of(startupTimeout, SECONDS)));

        mongoDBContainer.start();

        citrus.getCitrusContext().bind("mongoDBContainer", mongoDBContainer);

        setConnectionSettings(mongoDBContainer, context);

        if (TestContainersSteps.autoRemoveResources) {
            runner.run(doFinally()
                    .actions(context -> mongoDBContainer.stop()));
        }
    }

    @Given("^stop MongoDB container$")
    public void stopMongo() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }

        env = new HashMap<>();
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param mongoDBContainer
     * @param context
     */
    private void setConnectionSettings(MongoDBContainer mongoDBContainer, TestContext context) {
        if (mongoDBContainer.isRunning()) {
            String containerId = mongoDBContainer.getContainerId().substring(0, 12);

            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_CONTAINER_IP", mongoDBContainer.getHost());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_CONTAINER_ID", containerId);
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_CONTAINER_NAME", mongoDBContainer.getContainerName());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_SERVICE_PORT", mongoDBContainer.getMappedPort(27017));
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_SERVICE_LOCAL_URL", mongoDBContainer.getReplicaSetUrl());
            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_LOCAL_URL", mongoDBContainer.getReplicaSetUrl());

            if (YaksSettings.isLocal() || !TestContainersSettings.isKubedockEnabled()) {
                context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_SERVICE_NAME", "mongodb");
                context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_SERVICE_URL", mongoDBContainer.getReplicaSetUrl());
                context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_URL", mongoDBContainer.getReplicaSetUrl());
            } else {
                context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_SERVICE_NAME", String.format("kd-%s", containerId));
                context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_SERVICE_URL", String.format("mongodb://kd-%s:%d/test", containerId, mongoDBContainer.getMappedPort(27017)));
                context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_URL", String.format("mongodb://kd-%s:%d/test", containerId, mongoDBContainer.getMappedPort(27017)));
            }


            context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "MONGODB_KUBE_DOCK_HOST", String.format("kd-%s", containerId));
        }
    }
}
