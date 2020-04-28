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

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.assertj.core.api.Assertions;
import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class EnvironmentSettingRepositoryLoaderTest {

    private ConsoleLogger logger = new ConsoleLogger();
    @Test
    public void shouldLoadFromEnv() throws LifecycleExecutionException {
        EnvironmentSettingRepositoryLoader loader = new EnvironmentSettingRepositoryLoader() {
            @Override
            protected String getEnvSetting(String name) {
                return "central=https://repo.maven.apache.org/maven2/,jboss-ea=https://repository.jboss.org/nexus/content/groups/ea/";
            }
        };

        List<Repository> repositoryList = loader.load(logger);
        TestHelper.verifyRepositories(repositoryList);
    }

    @Test
    public void shouldHandleNonExistingSystemProperty() throws LifecycleExecutionException {
        EnvironmentSettingRepositoryLoader loader = new EnvironmentSettingRepositoryLoader();
        List<Repository> repositoryList = loader.load(logger);
        Assertions.assertThat(repositoryList).isEmpty();
    }
}
