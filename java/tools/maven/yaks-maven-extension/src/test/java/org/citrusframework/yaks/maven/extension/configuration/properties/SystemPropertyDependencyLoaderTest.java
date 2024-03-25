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

package org.citrusframework.yaks.maven.extension.configuration.properties;

import java.util.List;
import java.util.Properties;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.assertj.core.api.Assertions;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class SystemPropertyDependencyLoaderTest {

    private final SystemPropertyDependencyLoader loader = new SystemPropertyDependencyLoader();

    private final ConsoleLogger logger = new ConsoleLogger();
    private final Properties properties = new Properties();

    @Test
    public void shouldLoadFromSystemProperties() throws LifecycleExecutionException {

        System.setProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "org.foo:foo-artifact:1.0.0,org.bar:bar-artifact:1.5.0");

        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldLoadFromSystemPropertiesWithVersionResolving() throws LifecycleExecutionException {

        System.setProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "org.foo:foo-artifact:@foo.version@,org.bar:bar-artifact:@bar.version@");

        properties.put("foo.version", "1.0.0");
        properties.put("bar.version", "1.5.0");

        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldHandleNonExistingSystemProperty() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.DEPENDENCIES_SETTING_KEY, "");
        List<Dependency> dependencyList = loader.load(properties, logger);
        Assertions.assertThat(dependencyList).isEmpty();
    }

}
