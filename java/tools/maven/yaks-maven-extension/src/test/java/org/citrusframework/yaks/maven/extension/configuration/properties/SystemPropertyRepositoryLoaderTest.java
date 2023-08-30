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

import java.util.List;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.assertj.core.api.Assertions;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.TestHelper;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class SystemPropertyRepositoryLoaderTest {

    private final SystemPropertyRepositoryLoader loader = new SystemPropertyRepositoryLoader();

    private final ConsoleLogger logger = new ConsoleLogger();

    @Test
    public void shouldLoadFromSystemProperties() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.REPOSITORIES_SETTING_KEY,
                "central=https://repo.maven.apache.org/maven2/,jboss-ea=https://repository.jboss.org/nexus/content/groups/ea/");

        List<Repository> repositoryList = loader.load(logger, false);
        TestHelper.verifyRepositories(repositoryList);
    }

    @Test
    public void shouldHandleNonExistingSystemProperty() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.REPOSITORIES_SETTING_KEY, "");
        List<Repository> repositoryList = loader.load(logger, false);
        Assertions.assertThat(repositoryList).isEmpty();
    }

}
