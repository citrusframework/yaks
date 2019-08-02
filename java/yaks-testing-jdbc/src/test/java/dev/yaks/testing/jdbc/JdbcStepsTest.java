package dev.yaks.testing.jdbc;

import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
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
                  "dev.yaks.testing.jdbc" },
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
