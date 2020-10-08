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

import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.LoggingConfigurationLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * Loader reads additional dependency coordinates from system property setting. If system property is present the loader
 * expects the value to be a comma separated list of Maven coordinate scalars of form 'groupId:artifactId:version'.
 *
 * @author Christoph Deppisch
 */
public class SystemPropertyLoggingConfigurationLoader implements LoggingConfigurationLoader {

    @Override
    public Optional<Level> load(ConfigurationBuilder<BuiltConfiguration> builder, Logger logger) throws LifecycleExecutionException {
        Level rootLevel = null;
        String loggers = System.getProperty(ExtensionSettings.LOGGERS_SETTING_KEY, "");

        if (loggers.length() > 0) {
            for (String configuration : loggers.split(",")) {
                String loggerName=configuration.split("=")[0];
                String level=configuration.split("=")[1];
                rootLevel = configureLogger(loggerName, level, builder, logger).orElse(rootLevel);
            }
        }

        return Optional.ofNullable(rootLevel);
    }
}
