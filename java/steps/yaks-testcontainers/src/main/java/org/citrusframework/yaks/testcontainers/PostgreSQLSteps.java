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

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import org.apache.commons.dbcp2.BasicDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.DockerImageName;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;

public class PostgreSQLSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusResource
    private TestContext context;

    private String postgreSQLVersion = TestContainersSettings.getPostgreSQLVersion();

    private PostgreSQLContainer<?> postgreSQLContainer;

    @Before
    public void before(Scenario scenario) {
        if (postgreSQLContainer == null && citrus.getCitrusContext().getReferenceResolver().isResolvable(PostgreSQLContainer.class)) {
            postgreSQLContainer = citrus.getCitrusContext().getReferenceResolver().resolve("postgreSQLContainer", PostgreSQLContainer.class);
            setConnectionSettings(postgreSQLContainer, context);
        }
    }

    @Given("^PostgreSQL version (^\\s+)$")
    public void setPostgreSQLVersion(String version) {
        this.postgreSQLVersion = version;
    }

    @Given("^start PostgreSQL container$")
    public void startPostgresql() {
        postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres").withTag(postgreSQLVersion));

        postgreSQLContainer.start();

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
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_CONTAINER_IP", postgreSQLContainer.getContainerIpAddress());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_CONTAINER_NAME", postgreSQLContainer.getContainerName());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_PORT", String.valueOf(postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_URL", postgreSQLContainer.getJdbcUrl());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_USERNAME", postgreSQLContainer.getUsername());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_PASSWORD", postgreSQLContainer.getPassword());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_DRIVER", postgreSQLContainer.getDriverClassName());
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "POSTGRESQL_DB_NAME", postgreSQLContainer.getDatabaseName());
    }
}
