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

import java.util.List;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.assertj.core.api.Assertions;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class FileBasedPluginRepositoryLoaderTest {

    private final FileBasedRepositoryLoader loader = new FileBasedRepositoryLoader();

    private final ConsoleLogger logger = new ConsoleLogger();

    @Test
    public void shouldLoadFromPropertyFile() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.properties");
        List<Repository> repositoryList = loader.load(logger, true);
        TestHelper.verifyRepositories(repositoryList);

        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.settings.yaml");
        repositoryList = loader.load(logger, true);
        TestHelper.verifyRepositories(repositoryList);

        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "classpath:yaks.settings.json");
        repositoryList = loader.load(logger, true);
        TestHelper.verifyRepositories(repositoryList);
    }

    @Test
    public void shouldHandleNonExistingFile() throws LifecycleExecutionException {
        System.setProperty(ExtensionSettings.SETTINGS_FILE_KEY, "doesNotExist");
        List<Repository> repositoryList = loader.load(logger, true);
        Assertions.assertThat(repositoryList).isEmpty();
    }

}
