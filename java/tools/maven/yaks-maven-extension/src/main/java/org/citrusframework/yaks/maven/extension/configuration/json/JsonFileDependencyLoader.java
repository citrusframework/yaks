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

package org.citrusframework.yaks.maven.extension.configuration.json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileDependencyLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * Load dependencies from Json configuration file. The configuration should reside as list of Maven artifact dependencies.
 *
 * {
 *   "dependencies": [
 *     {
 *       "groupId": "org.foo",
 *       "artifactId": "foo-artifact",
 *       "version": "1.0.0"
 *     },
 *     {
 *       "groupId": "org.bar",
 *       "artifactId": "bar-artifact",
 *       "version": "1.5.0"
 *     }
 *   ]
 * }
 *
 * Each dependency value should be a proper Maven coordinate with groupId, artifactId and version.
 * @author Christoph Deppisch
 */
public class JsonFileDependencyLoader extends AbstractConfigFileDependencyLoader {

    @Override
    protected List<Dependency> load(Path filePath, Properties properties, Logger logger) throws LifecycleExecutionException {
        List<Dependency> dependencyList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            ArrayNode dependencies = (ArrayNode) root.get("dependencies");
            for (Object o : dependencies) {
                ObjectNode coordinates = (ObjectNode) o;
                Dependency dependency = new Dependency();

                dependency.setGroupId(coordinates.get("groupId").textValue());
                dependency.setArtifactId(coordinates.get("artifactId").textValue());
                dependency.setVersion(resolveVersionProperty(coordinates.get("version").textValue(), properties));

                logger.info(String.format("Add %s", dependency));
                dependencyList.add(dependency);
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read json dependency config file", e);
        }

        return dependencyList;
    }
}
