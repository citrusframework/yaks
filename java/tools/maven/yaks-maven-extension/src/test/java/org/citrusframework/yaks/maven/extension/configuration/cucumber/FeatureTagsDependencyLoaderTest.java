package org.citrusframework.yaks.maven.extension.configuration.cucumber;

import java.util.List;
import java.util.Properties;

import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class FeatureTagsDependencyLoaderTest {

    private FeatureTagsDependencyLoader loader = new FeatureTagsDependencyLoader();

    private ConsoleLogger logger = new ConsoleLogger();
    private Properties properties = new Properties();

    @Test
    public void shouldLoadFromPropertyFile() throws LifecycleExecutionException {
        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

}
