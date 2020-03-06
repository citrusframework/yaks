package dev.yaks.maven.extension.configuration;

import java.util.List;
import java.util.Properties;

import dev.yaks.maven.extension.ExtensionSettings;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.assertj.core.api.Assertions;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class FileBasedDependencyLoaderTest {

    private FileBasedDependencyLoader loader = new FileBasedDependencyLoader();

    private ConsoleLogger logger = new ConsoleLogger();
    private Properties properties = new Properties();

    @Test
    public void shouldLoadFromPropertyFile() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.properties");
        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);

        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.settings.yaml");
        dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);

        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.settings.json");
        dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldHandleNonExistingFile() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "doesNotExist");
        List<Dependency> dependencyList = loader.load(properties, logger);
        Assertions.assertThat(dependencyList).isEmpty();
    }

}
