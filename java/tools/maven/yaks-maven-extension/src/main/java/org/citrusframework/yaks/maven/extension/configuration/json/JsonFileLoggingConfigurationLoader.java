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
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileLoggingConfigurationLoader;
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
public class JsonFileLoggingConfigurationLoader extends AbstractConfigFileLoggingConfigurationLoader {

    @Override
    protected Optional<Level> load(Path filePath, ConfigurationBuilder<BuiltConfiguration> builder, Logger logger) throws LifecycleExecutionException {
        Level rootLevel = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            ArrayNode loggers = (ArrayNode) root.get("loggers");
            for (Object o : loggers) {
                ObjectNode configuration = (ObjectNode) o;
                String loggerName = configuration.get("name").textValue();
                String level = configuration.get("level").textValue();
                rootLevel = configureLogger(loggerName, level, builder, logger).orElse(rootLevel);
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read json config file", e);
        }

        return Optional.ofNullable(rootLevel);
    }
}
