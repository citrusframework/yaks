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

package org.citrusframework.yaks.maven.extension.configuration.properties;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileLoggingConfigurationLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Christoph Deppisch
 */
public class PropertyFileLoggingConfigurationLoader extends AbstractConfigFileLoggingConfigurationLoader {

    @Override
    protected Optional<Level> load(Path filePath, ConfigurationBuilder<BuiltConfiguration> builder, Logger logger) throws LifecycleExecutionException {
        Level rootLevel = null;
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getValue() == null || !entry.getKey().toString().startsWith(ExtensionSettings.LOGGING_LEVEL_PREFIX)) {
                    continue;
                }

                String loggerName = entry.getKey().toString().substring(ExtensionSettings.LOGGING_LEVEL_PREFIX.length());
                String level = entry.getValue().toString();
                rootLevel = configureLogger(loggerName, level, builder, logger).orElse(rootLevel);
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to load properties from configuration file", e);
        }

        return Optional.ofNullable(rootLevel);
    }
}
