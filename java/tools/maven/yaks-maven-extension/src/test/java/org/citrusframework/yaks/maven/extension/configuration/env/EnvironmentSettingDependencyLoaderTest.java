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

package org.citrusframework.yaks.maven.extension.configuration.env;

import java.util.List;
import java.util.Properties;

import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.assertj.core.api.Assertions;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class EnvironmentSettingDependencyLoaderTest {

    private ConsoleLogger logger = new ConsoleLogger();
    private Properties properties = new Properties();

    @Test
    public void shouldLoadFromEnv() throws LifecycleExecutionException {
        EnvironmentSettingDependencyLoader loader = new EnvironmentSettingDependencyLoader() {
            @Override
            protected String getEnvSetting(String name) {
                return "org.foo:foo-artifact:1.0.0,org.bar:bar-artifact:1.5.0";
            }
        };

        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldLoadFromEnvWithVersionResolving() throws LifecycleExecutionException {
        EnvironmentSettingDependencyLoader loader = new EnvironmentSettingDependencyLoader() {
            @Override
            protected String getEnvSetting(String name) {
                return "org.foo:foo-artifact:@foo.version@,org.bar:bar-artifact:@bar.version@";
            }
        };

        properties.put("foo.version", "1.0.0");
        properties.put("bar.version", "1.5.0");

        List<Dependency> dependencyList = loader.load(properties, logger);
        TestHelper.verifyDependencies(dependencyList);
    }

    @Test
    public void shouldHandleNonExistingSystemProperty() throws LifecycleExecutionException {
        EnvironmentSettingDependencyLoader loader = new EnvironmentSettingDependencyLoader();
        List<Dependency> dependencyList = loader.load(properties, logger);
        Assertions.assertThat(dependencyList).isEmpty();
    }
}
