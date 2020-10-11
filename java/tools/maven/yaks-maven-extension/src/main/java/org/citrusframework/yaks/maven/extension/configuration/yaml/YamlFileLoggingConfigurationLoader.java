/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileLoggingConfigurationLoader;
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
public class YamlFileLoggingConfigurationLoader extends AbstractConfigFileLoggingConfigurationLoader {

    @Override
    protected Optional<Level> load(Path filePath, ConfigurationBuilder<BuiltConfiguration> builder, Logger logger) throws LifecycleExecutionException {
        Level rootLevel = null;
        try {
            Yaml yaml = new Yaml();

            HashMap<String, List<Map<String, Object>>> root = yaml.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            if (root.containsKey("loggers")) {
                for (Map<String, Object> configuration : root.get("loggers")) {
                    String loggerName = Objects.toString(configuration.get("name"));
                    String level = Objects.toString(configuration.get("level"));
                    rootLevel = configureLogger(loggerName, level, builder, logger).orElse(rootLevel);
                }
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read dependency configuration file", e);
        }

        return Optional.ofNullable(rootLevel);
    }
}
