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

package org.citrusframework.yaks.maven.extension.configuration.yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileRepositoryLoader;
import org.codehaus.plexus.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Yaml configuration file loader is supposed to have one to many entries that unmarshal to a Maven repository model:
 *
 * repositories:
 *   - repository:
 *       id: "central"
 *       name: "Maven Central"
 *       url: "https://repo.maven.apache.org/maven2/"
 *       releases:
 *         enabled: "true"
 *         updatePolicy: "daily"
 *       snapshots:
 *         enabled: "false"
 *   - repository:
 *       id: "jboss-ea"
 *       name: "JBoss Community Early Access Release Repository"
 *       url: "https://repository.jboss.org/nexus/content/groups/ea/"
 *       layout: "default"
 *
 * Each repository value should be a proper Maven coordinate with groupId, artifactId and version.
 * @author Christoph Deppisch
 */
public class YamlFileRepositoryLoader extends AbstractConfigFileRepositoryLoader {

    @Override
    protected List<Repository> load(Path filePath, Logger logger) throws LifecycleExecutionException {
        try {
            List<Repository> repositoryList = new ArrayList<>();
            Yaml yaml = new Yaml();

            HashMap<String, List<Map<String, Map<String, Object>>>> root = yaml.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            if (root.containsKey("repositories")) {
                List<Map<String, Object>> repositories = root.get("repositories").stream()
                        .filter(d -> d.containsKey("repository"))
                        .map(d -> d.get("repository"))
                        .collect(Collectors.toList());

                for (Map<String, Object> model : repositories) {
                    Repository repository = new Repository();

                    repository.setId(model.get("id").toString());
                    repository.setName(model.get("name").toString());
                    repository.setUrl(model.get("url").toString());

                    logger.info(String.format("Add Repository %s=%s", repository.getId(), repository.getUrl()));
                    repositoryList.add(repository);
                }
            }

            return repositoryList;
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read repository configuration file", e);
        }
    }
}
