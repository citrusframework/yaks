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

import javax.script.ScriptException;
import java.time.Duration;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.DockerImageName;

import static com.consol.citrus.container.Catch.Builder.catchException;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static com.consol.citrus.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.citrusframework.yaks.kubernetes.actions.KubernetesActionBuilder.kubernetes;

public class PostgreSQLSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    private String postgreSQLVersion = PostgreSQLSettings.getPostgreSQLVersion();

    private PostgreSQLContainer<?> postgreSQLContainer;

    private String databaseName = PostgreSQLSettings.getDatabaseName();
    private String username = PostgreSQLSettings.getUsername();
    private String password = PostgreSQLSettings.getPassword();

    private int startupTimeout = PostgreSQLSettings.getStartupTimeout();

    private KubernetesClient k8sClient;

    @Before
    public void before(Scenario scenario) {
        if (postgreSQLContainer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(PostgreSQLContainer.class)) {
            postgreSQLContainer = citrus.getCitrusContext().getReferenceResolver().resolve("postgreSQLContainer", PostgreSQLContainer.class);
            setConnectionSettings(postgreSQLContainer, context);
        }

        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^PostgreSQL version (^\\s+)$")
    public void setPostgreSQLVersion(String version) {
        this.postgreSQLVersion = version;
    }

    @Given("^PostgreSQL startup timeout is (\\d+)(?: s| seconds)$")
    public void setStartupTimeout(int timeout) {
        this.startupTimeout = timeout;
    }

    @Given("^PostgreSQL database name (^\\s+)$")
    public void setDatabaseName(String name) {
        this.databaseName = name;
    }

    @Given("^PostgreSQL username (^\\s+)$")
    public void setUsername(String name) {
        this.username = name;
    }

    @Given("^PostgreSQL password (^\\s+)$")
    public void setPassword(String password) {
        this.password = password;
    }

    @Given("^start PostgreSQL container$")
    public void startPostgresql() {
        postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag(postgreSQLVersion))
                .withUsername(username)
                .withPassword(password)
                .withDatabaseName(databaseName)
                .withLabel("app", "yaks")
                .withLabel("app.openshift.io/connects-to", TestContainersSettings.getTestId())
                .withNetworkAliases("postgresql")
                .waitingFor(Wait.forListeningPort()
                        .withStartupTimeout(Duration.of(startupTimeout, SECONDS)));

        postgreSQLContainer.start();

        runner.run(catchException()
                .exception(KubernetesClientException.class)
                .when(repeatOnError()
                    .until((index, context) -> index >= 25)
                    .autoSleep(1000L)
                    .actions(
                        kubernetes().client(k8sClient).deployments()
                            .addLabel(postgreSQLContainer.getContainerId())
                            .label("app.kubernetes.io/name", "postgresql")
                            .label("app.openshift.io/part-of", TestContainersSettings.getTestName())
                )));

        String initScript = DatabaseContainerSteps.getInitScript(context);
        if (!initScript.isEmpty()) {
            try {
                ScriptUtils.executeDatabaseScript(new JdbcDatabaseDelegate(postgreSQLContainer, ""), "init.sql", initScript);
            } catch (ScriptException e) {
                throw new CitrusRuntimeException("Failed to execute init script");
            }
        }

        BasicDataSource postgreSQLDataSource = new BasicDataSource();
        postgreSQLDataSource.setDriverClassName(postgreSQLContainer.getDriverClassName());
        postgreSQLDataSource.setUrl(postgreSQLContainer.getJdbcUrl());
        postgreSQLDataSource.setUsername(postgreSQLContainer.getUsername());
        postgreSQLDataSource.setPassword(postgreSQLContainer.getPassword());

        citrus.getCitrusContext().bind("postgreSQL", postgreSQLDataSource);
        citrus.getCitrusContext().bind("postgreSQLContainer", postgreSQLContainer);

        setConnectionSettings(postgreSQLContainer, context);

        if (TestContainersSteps.autoRemoveResources) {
            runner.run(doFinally()
                    .actions(context -> postgreSQLContainer.stop()));
        }
    }

    @Given("^stop PostgreSQL container$")
    public void stopPostgresql() {
        if (postgreSQLContainer != null) {
            postgreSQLContainer.stop();
        }
    }

    /**
     * Sets the connection settings in current test context in the form of test variables.
     * @param postgreSQLContainer
     * @param context
     */
    private void setConnectionSettings(PostgreSQLContainer<?> postgreSQLContainer, TestContext context) {
        String containerId = postgreSQLContainer.getContainerId().substring(0, 12);

        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_CONTAINER_IP", postgreSQLContainer.getContainerIpAddress());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_CONTAINER_ID", containerId);
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_CONTAINER_NAME", postgreSQLContainer.getContainerName());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_SERVICE_NAME", "kd-" + containerId);
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_PORT", String.valueOf(postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_URL", postgreSQLContainer.getJdbcUrl());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_USERNAME", postgreSQLContainer.getUsername());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_PASSWORD", postgreSQLContainer.getPassword());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_DRIVER", postgreSQLContainer.getDriverClassName());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_DB_NAME", postgreSQLContainer.getDatabaseName());
    }
}
