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
import java.util.Objects;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileRepositoryLoader;
import org.codehaus.plexus.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Yaml configuration file loader is supposed to have one to many entries that unmarshal to a Maven repository model:
 *
 * repositories:
 *   - id: "central"
 *     name: "Maven Central"
 *     url: "https://repo.maven.apache.org/maven2/"
 *     releases:
 *       enabled: "true"
 *       updatePolicy: "daily"
 *     snapshots:
 *       enabled: "false"
 *   - id: "jboss-ea"
 *     name: "JBoss Community Early Access Release Repository"
 *     url: "https://repository.jboss.org/nexus/content/groups/ea/"
 *     layout: "default"
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

            HashMap<String, List<Map<String, Object>>> root = yaml.load(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            if (root.containsKey("repositories")) {
                for (Map<String, Object> model : root.get("repositories")) {
                    Repository repository = new Repository();

                    repository.setId(Objects.toString(model.get("id")));
                    repository.setName(Objects.toString(model.getOrDefault("name", repository.getId())));
                    repository.setUrl(Objects.toString(model.get("url")));
                    repository.setLayout(Objects.toString(model.getOrDefault("layout", "default")));

                    if (model.containsKey("releases")) {
                        repository.setReleases(getRepositoryPolicy((Map<String, Object>) model.get("releases")));
                    }

                    if (model.containsKey("snapshots")) {
                        repository.setReleases(getRepositoryPolicy((Map<String, Object>) model.get("snapshots")));
                    }

                    logger.info(String.format("Add Repository %s=%s", repository.getId(), repository.getUrl()));
                    repositoryList.add(repository);
                }
            }

            return repositoryList;
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read repository configuration file", e);
        }
    }

    /**
     * Construct repository policy from given key-value model.
     * @param policyModel
     * @return
     */
    private RepositoryPolicy getRepositoryPolicy(Map<String, Object> policyModel) {
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(Objects.toString(policyModel.getOrDefault("enabled", "true")));
        policy.setUpdatePolicy(Objects.toString(policyModel.getOrDefault("updatePolicy", "always")));
        return policy;
    }
}
