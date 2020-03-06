package dev.yaks.maven.extension.configuration.properties;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import dev.yaks.maven.extension.ExtensionSettings;
import dev.yaks.maven.extension.configuration.TestHelper;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.assertj.core.api.Assertions;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class SystemPropertyDependencyLoaderTest {

    private SystemPropertyDependencyLoader loader = new SystemPropertyDependencyLoader();

    private ConsoleLogger logger = new ConsoleLogger();
    private Properties properties = new Properties();

    @Test
    public void shouldLoadFromSystemProperties() throws LifecycleExecutionException, URISyntaxException {

        System.setProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "org.foo:foo-artifact:1.0.0,org.bar:bar-artifact:1.5.0");

        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldLoadFromSystemPropertiesWithVersionResolving() throws LifecycleExecutionException, URISyntaxException {

        System.setProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "org.foo:foo-artifact:@foo.version@,org.bar:bar-artifact:@bar.version@");

        properties.put("foo.version", "1.0.0");
        properties.put("bar.version", "1.5.0");

        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldHandleNonExistingSystemProperty() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "");
        List<Dependency> dependencyList = loader.load(properties, logger);
        Assertions.assertThat(dependencyList).isEmpty();
    }

}
