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
import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class SystemPropertyLoggingConfigurationLoaderTest {

    private final SystemPropertyLoggingConfigurationLoader loader = new SystemPropertyLoggingConfigurationLoader();

    private final ConsoleLogger logger = new ConsoleLogger();
    private final ConfigurationBuilder<BuiltConfiguration> builder = LoggingConfigurationLoader.newConfigurationBuilder();

    @Test
    public void shouldLoadFromSystemProperties() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.LOGGERS_SETTING_KEY, "root=info,org.foo=debug,org.bar=warn");

        Optional<Level> rootLevel = loader.load(builder, logger);
        Assert.assertTrue(rootLevel.isPresent());
        Assert.assertEquals(Level.INFO, rootLevel.get());

        TestHelper.verifyLoggingConfiguration(builder);
    }

    @Test
    public void shouldHandleNonExistingSystemProperty() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.LOGGERS_SETTING_KEY, "");
        Optional<Level> rootLevel = loader.load(builder, logger);
        Assert.assertFalse(rootLevel.isPresent());
        TestHelper.verifyDefaultLoggingConfiguration(builder);
    }

}
