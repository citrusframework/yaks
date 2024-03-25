/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileDependencyLoader;
import org.codehaus.plexus.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Yaml configuration file loader is supposed to have one to many entries that unmarshal to a Maven dependency model:
 *
 * dependencies
 *   - groupId: org.foo
 *     artifactId: foo
 *     version: 1.0.0
 *   - groupId: org.bar
 *     artifactId: bar
 *     version: 1.2.0
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

            HashMap<String, List<Map<String, Object>>> root = yaml.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            if (root.containsKey("dependencies")) {
                for (Map<String, Object> coordinates : root.get("dependencies")) {
                    Dependency dependency = new Dependency();
                    dependency.setGroupId(Objects.toString(coordinates.get("groupId")));
                    dependency.setArtifactId(Objects.toString(coordinates.get("artifactId")));
                    dependency.setVersion(resolveVersionProperty(Objects.toString(coordinates.get("version")), properties));

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
