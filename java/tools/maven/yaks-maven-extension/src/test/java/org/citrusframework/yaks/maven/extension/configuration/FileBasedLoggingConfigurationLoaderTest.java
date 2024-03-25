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

import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class FileBasedLoggingConfigurationLoaderTest {

    private final FileBasedLoggingConfigurationLoader loader = new FileBasedLoggingConfigurationLoader();

    private final ConsoleLogger logger = new ConsoleLogger();
    private ConfigurationBuilder<BuiltConfiguration> builder;

    @Test
    public void shouldLoadFromPropertyFile() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.properties");
        builder = LoggingConfigurationLoader.newConfigurationBuilder();
        Optional<Level> rootLevel = loader.load(builder, logger);
        Assert.assertTrue(rootLevel.isPresent());
        Assert.assertEquals(Level.INFO, rootLevel.get());
        TestHelper.verifyLoggingConfiguration(builder);

        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.settings.yaml");
        builder = LoggingConfigurationLoader.newConfigurationBuilder();
        rootLevel = loader.load(builder, logger);
        Assert.assertTrue(rootLevel.isPresent());
        Assert.assertEquals(Level.INFO, rootLevel.get());
        TestHelper.verifyLoggingConfiguration(builder);

        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.settings.json");
        builder = LoggingConfigurationLoader.newConfigurationBuilder();
        rootLevel = loader.load(builder, logger);
        Assert.assertTrue(rootLevel.isPresent());
        Assert.assertEquals(Level.INFO, rootLevel.get());
        TestHelper.verifyLoggingConfiguration(builder);
    }

    @Test
    public void shouldHandleNonExistingFile() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "doesNotExist");
        builder = LoggingConfigurationLoader.newConfigurationBuilder();
        Optional<Level> rootLevel = loader.load(builder, logger);
        Assert.assertFalse(rootLevel.isPresent());
        TestHelper.verifyDefaultLoggingConfiguration(builder);
    }

}
