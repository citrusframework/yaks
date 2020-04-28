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

package org.citrusframework.yaks.maven.extension;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.citrusframework.yaks.maven.extension.configuration.FileBasedRepositoryLoader;
import org.citrusframework.yaks.maven.extension.configuration.env.EnvironmentSettingRepositoryLoader;
import org.citrusframework.yaks.maven.extension.configuration.properties.SystemPropertyRepositoryLoader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Model reader extends default reader and adds dynamic Maven repositories loaded from different configuration sources.
 * This way the extension can add configuration as if it was added to the pom.xml.
 *
 * @author Christoph Deppisch
 */
@Component(role = ModelReader.class)
@Singleton
public class ProjectModelReader extends DefaultModelReader {

    @Requirement
    private Logger logger;

    private List<Repository> repositoryList;

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        Model projectModel = super.read(input, options);
        projectModel.getRepositories().addAll(loadDynamicRepositories());
        return projectModel;
    }

    /**
     * Dynamically add project repositories based on different configuration sources such as environment variables,
     * system properties configuration files.
     */
    private List<Repository> loadDynamicRepositories() {
        if (repositoryList == null) {
            repositoryList = new ArrayList<>();
            logger.info("Add dynamic project repositories ...");

            try {
                repositoryList.addAll(new FileBasedRepositoryLoader().load(logger));
                repositoryList.addAll(new SystemPropertyRepositoryLoader().load(logger));
                repositoryList.addAll(new EnvironmentSettingRepositoryLoader().load(logger));
            } catch (LifecycleExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return repositoryList;
    }
}
