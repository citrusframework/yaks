package org.citrusframework.yaks.maven.extension.configuration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractConfigFileDependencyLoader implements DependencyLoader  {

    @Override
    public List<Dependency> load(Properties properties, Logger logger) {
        return Collections.emptyList();
    }

    /**
     * Load dependencies from given file.
     * @param filePath
     * @param properties
     * @param logger
     * @return
     * @throws LifecycleExecutionException
     */
    protected abstract List<Dependency> load(Path filePath, Properties properties, Logger logger) throws LifecycleExecutionException;
}
