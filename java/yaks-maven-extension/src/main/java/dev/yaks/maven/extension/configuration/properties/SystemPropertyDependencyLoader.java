package dev.yaks.maven.extension.configuration.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dev.yaks.maven.extension.ExtensionSettings;
import dev.yaks.maven.extension.configuration.DependencyLoader;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * Loader reads additional dependency coordinates from system property setting. If system property is present the loader
 * expects the value to be a comma separated list of Maven coordinate scalars of form 'groupId:artifactId:version'.
 *
 * @author Christoph Deppisch
 */
public class SystemPropertyDependencyLoader implements DependencyLoader {

    @Override
    public List<Dependency> load(Properties properties, Logger logger) throws LifecycleExecutionException {
        List<Dependency> dependencyList = new ArrayList<>();

        String coordinates = System.getProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "");

        if (coordinates.length() > 0) {
            for (String coordinate : coordinates.split(",")) {
                dependencyList.add(build(coordinate, properties, logger));
            }

            if (!dependencyList.isEmpty()) {
                logger.info(String.format("Add %s dependencies found in system properties", dependencyList.size()));
            }
        }

        return dependencyList;
    }
}
