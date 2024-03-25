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

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileRepositoryLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * Property file based configuration loader. Property file is supposed to have one to many entries following the property name prefix:
 *
 * yaks.repository.central=https://repo.maven.apache.org/maven2/
 * yaks.repository.jboss-ea=https://repository.jboss.org/nexus/content/groups/ea/
 *
 * Each property value should be a proper Maven coordinate with groupId, artifactId and version.
 *
 * @author Christoph Deppisch
 */
public class PropertyFileRepositoryLoader extends AbstractConfigFileRepositoryLoader {

    @Override
    protected List<Repository> load(Path filePath, Logger logger, boolean asPluginRepository) throws LifecycleExecutionException {
        List<Repository> repositoryList = new ArrayList<>();

        try {
            Properties props = new Properties();
            props.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));

            for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
                String name = (String) e.nextElement();
                if (asPluginRepository && name.startsWith("yaks.pluginRepository.")) {
                    repositoryList.add(build(name.substring("yaks.pluginRepository.".length()), props.getProperty(name), logger));
                } else if (!asPluginRepository && name.startsWith("yaks.repository.")) {
                    repositoryList.add(build(name.substring("yaks.repository.".length()), props.getProperty(name), logger));
                }
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to load properties from repository configuration file", e);
        }

        return repositoryList;
    }
}
