package org.citrusframework.yaks.maven.extension.configuration.properties;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileDependencyLoader;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * Property file based configuration loader. Property file is supposed to have one to many entries following the property name prefix:
 *
 * yaks.dependency.foo=org.foo:foo:1.0.0
 * yaks.dependency.bar=org.bar:bar:1.2.0
 *
 * Each property value should be a proper Maven coordinate with groupId, artifactId and version.
 *
 * @author Christoph Deppisch
 */
public class PropertyFileDependencyLoader extends AbstractConfigFileDependencyLoader {

    @Override
    protected List<Dependency> load(Path filePath, Properties properties, Logger logger) throws LifecycleExecutionException {
        List<Dependency> dependencyList = new ArrayList<>();

        try {
            Properties props = new Properties();
            props.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));

            for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
                String name = (String) e.nextElement();
                if (name.startsWith("yaks.dependency.")) {
                    dependencyList.add(build(props.getProperty(name), properties, logger));
                }
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to load properties from dependency configuration file", e);
        }

        return dependencyList;
    }
}
