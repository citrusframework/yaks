package org.citrusframework.yaks.maven.extension.configuration.json;

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
public class JsonFileDependencyLoaderTest {

    private JsonFileDependencyLoader loader = new JsonFileDependencyLoader();

    private ConsoleLogger logger = new ConsoleLogger();
    private Properties properties = new Properties();

    @Test
    public void shouldLoadFromJson() throws LifecycleExecutionException, URISyntaxException {
        List<Dependency> dependencyList = loader.load(TestHelper.getClasspathResource("yaks.settings.json"), properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldHandleNonExistingJson() {
        Assertions.assertThatExceptionOfType(LifecycleExecutionException.class)
                .isThrownBy(() -> loader.load(Paths.get("doesNotExist"), properties, logger));
    }

}
