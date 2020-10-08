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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.RepositoryLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * Loader reads additional Maven repositories from an environment setting. If environment setting is present the loader
 * expects the value to be a comma separated list of Maven repository scalars of form 'id=url'.
 *
 * @author Christoph Deppisch
 */
public class EnvironmentSettingRepositoryLoader implements RepositoryLoader, EnvironmentSettingLoader {

    @Override
    public List<Repository> load(Logger logger) throws LifecycleExecutionException {
        List<Repository> repositoryList = new ArrayList<>();

        String settings = getEnvSetting(ExtensionSettings.REPOSITORIES_SETTING_ENV);

        if (settings.length() > 0) {
            for (String scalar : settings.split(",")) {
                String[] config = scalar.split("=");
                if (config.length == 2) {
                    repositoryList.add(build(config[0], config[1], logger));
                }
            }

            if (!repositoryList.isEmpty()) {
                logger.info(String.format("Add %s repositories found in environment variables", repositoryList.size()));
            }
        }

        return repositoryList;
    }
}
