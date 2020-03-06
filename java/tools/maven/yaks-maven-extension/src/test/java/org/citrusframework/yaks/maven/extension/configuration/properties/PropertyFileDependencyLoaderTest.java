package org.citrusframework.yaks.maven.extension.configuration.properties;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.assertj.core.api.Assertions;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class PropertyFileDependencyLoaderTest {

    private PropertyFileDependencyLoader loader = new PropertyFileDependencyLoader();

    private ConsoleLogger logger = new ConsoleLogger();
    private Properties properties = new Properties();

    @Test
    public void shouldLoadFromPropertyFile() throws LifecycleExecutionException, URISyntaxException {
        List<Dependency> dependencyList = loader.load(TestHelper.getClasspathResource("yaks.properties"), properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldHandleNonExistingFile() {
        Assertions.assertThatExceptionOfType(LifecycleExecutionException.class)
                .isThrownBy(() -> loader.load(Paths.get("doesNotExist"), properties, logger));
    }

}
