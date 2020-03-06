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
import java.util.Optional;
import java.util.Properties;

import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.DependencyLoader;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * Loader reads additional dependency coordinates from an environment setting. If environment setting is present the loader
 * expects the value to be a comma separated list of Maven coordinate scalars of form 'groupId:artifactId:version'.
 *
 * @author Christoph Deppisch
 */
public class EnvironmentSettingDependencyLoader implements DependencyLoader {

    @Override
    public List<Dependency> load(Properties properties, Logger logger) throws LifecycleExecutionException {
        List<Dependency> dependencyList = new ArrayList<>();

        String coordinates = getEnvSetting(ExtensionSettings.DEPENDENCIES_SETTING_ENV);

        if (coordinates.length() > 0) {
            for (String coordinate : coordinates.split(",")) {
                dependencyList.add(build(coordinate, properties, logger));
            }

            if (!dependencyList.isEmpty()) {
                logger.info(String.format("Add %s dependencies found in environment variables", dependencyList.size()));
            }
        }

        return dependencyList;
    }

    /**
     * Read environment setting. If setting is not present default to empty value.
     * @param name
     * @return
     */
    protected String getEnvSetting(String name) {
        return Optional.ofNullable(System.getenv(name)).orElse("");
    }
}
