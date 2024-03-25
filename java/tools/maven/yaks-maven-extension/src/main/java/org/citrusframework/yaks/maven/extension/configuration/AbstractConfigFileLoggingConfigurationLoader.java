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

package org.citrusframework.yaks.maven.extension.configuration;

import java.nio.file.Path;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractConfigFileLoggingConfigurationLoader implements LoggingConfigurationLoader  {

    @Override
    public Optional<Level> load(ConfigurationBuilder<BuiltConfiguration> builder, Logger logger) {
        return Optional.empty();
    }

    /**
     * Load logger configuration from given file.
     * @param filePath
     * @param builder
     * @param logger
     * @return root logger level if any is set
     * @throws LifecycleExecutionException
     */
    protected abstract Optional<Level> load(Path filePath, ConfigurationBuilder<BuiltConfiguration> builder, Logger logger) throws LifecycleExecutionException;
}
