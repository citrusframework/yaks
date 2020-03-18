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

package org.citrusframework.yaks.jdbc;

import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "com.consol.citrus.cucumber.step.runner.core",
                  "org.citrusframework.yaks.jdbc" },
        plugin = { "com.consol.citrus.cucumber.CitrusReporter" } )
public class JdbcStepsTest {

    @ClassRule
    public static GenericContainer testdbContainer = new PostgreSQLContainer()
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("secret")
            .withInitScript("test-db-init.sql")
            .withCreateContainerCmdModifier((Consumer<CreateContainerCmd>) modifier -> modifier.withPortBindings(
                    new PortBinding(Ports.Binding.bindPort(PostgreSQLContainer.POSTGRESQL_PORT),
                    new ExposedPort(PostgreSQLContainer.POSTGRESQL_PORT))));
}
