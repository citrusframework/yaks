package org.citrusframework.yaks.maven.extension.configuration.yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileDependencyLoader;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Yaml configuration file loader is supposed to have one to many entries that unmarshal to a Maven dependency model:
 *
 * dependencies
 *   - dependency:
 *       groupId: org.foo
 *       artifactId: foo
 *       version: 1.0.0
 *   - dependency:
 *       groupId: org.bar
 *       artifactId: bar
 *       version: 1.2.0
 *
 * Each dependency value should be a proper Maven coordinate with groupId, artifactId and version.
 * @author Christoph Deppisch
 */
public class YamlFileDependencyLoader extends AbstractConfigFileDependencyLoader {

    @Override
    protected List<Dependency> load(Path filePath, Properties properties, Logger logger) throws LifecycleExecutionException {
        try {
            List<Dependency> dependencyList = new ArrayList<>();
            Yaml yaml = new Yaml();

            HashMap<String, List<Map<String, Map<String, Object>>>> root = yaml.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            if (root.containsKey("dependencies")) {
                List<Map<String, Object>> dependencies = root.get("dependencies").stream()
                        .filter(d -> d.containsKey("dependency"))
                        .map(d -> d.get("dependency"))
                        .collect(Collectors.toList());

                for (Map<String, Object> coordinates : dependencies) {
                    Dependency dependency = new Dependency();
                    dependency.setGroupId(coordinates.get("groupId").toString());
                    dependency.setArtifactId(coordinates.get("artifactId").toString());
                    dependency.setVersion(resolveVersionProperty(coordinates.get("version").toString(), properties));

                    logger.info(String.format("Add %s", dependency));
                    dependencyList.add(dependency);
                }
            }

            return dependencyList;
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read dependency configuration file", e);
        }
    }
}
