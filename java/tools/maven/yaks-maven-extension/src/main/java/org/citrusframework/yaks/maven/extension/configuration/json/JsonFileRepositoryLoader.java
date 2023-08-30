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

package org.citrusframework.yaks.maven.extension.configuration.json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.citrusframework.yaks.maven.extension.configuration.AbstractConfigFileRepositoryLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * Load repositories from Json configuration file. The configuration should reside as list of Maven artifact repositories.
 *
 * {
 *   "repositories": [
 *     {
 *       "id": "central",
 *       "name": "Maven Central",
 *       "url": "https://repo.maven.apache.org/maven2/",
 *       "releases": {
 *         "enabled": "true",
 *         "updatePolicy": "daily"
 *       },
 *       "snapshots": {
 *         "enabled": "false"
 *       }
 *     },
 *     {
 *       "id": "jboss-ea",
 *       "name": "JBoss Community Early Access Release Repository",
 *       "url": "https://repository.jboss.org/nexus/content/groups/ea/",
 *       "layout": "default"
 *     }
 *   ]
 * }
 *
 * Each repository value should be a proper Maven coordinate with groupId, artifactId and version.
 * @author Christoph Deppisch
 */
public class JsonFileRepositoryLoader extends AbstractConfigFileRepositoryLoader {

    @Override
    protected List<Repository> load(Path filePath, Logger logger, boolean asPluginRepository) throws LifecycleExecutionException {
        List<Repository> repositoryList = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            ArrayNode repositories = (ArrayNode) root.get(asPluginRepository ? "pluginRepositories" : "repositories");
            for (Object o : repositories) {
                ObjectNode model = (ObjectNode) o;
                Repository repository = new Repository();

                repository.setId(model.get("id").textValue());
                repository.setUrl(model.get("url").textValue());

                if (model.get("name") != null) {
                    repository.setName(model.get("name").textValue());
                }

                if (model.get("layout") != null) {
                    repository.setLayout(model.get("layout").textValue());
                }

                if (model.get("releases") != null) {
                    repository.setReleases(getRepositoryPolicy((ObjectNode) model.get("releases")));
                }

                if (model.get("snapshots") != null) {
                    repository.setReleases(getRepositoryPolicy((ObjectNode) model.get("snapshots")));
                }

                logger.info(String.format("Add Repository %s=%s", repository.getId(), repository.getUrl()));
                repositoryList.add(repository);
            }
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to read json repository config file", e);
        }

        return repositoryList;
    }

    /**
     * Construct repository policy from given key-value model.
     * @param policyModel
     * @return
     */
    private RepositoryPolicy getRepositoryPolicy(ObjectNode policyModel) {
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(Optional.ofNullable(policyModel.get("enabled")).map(Object::toString).orElse("true"));
        policy.setUpdatePolicy(Optional.ofNullable(policyModel.get("updatePolicy")).map(Objects::toString).orElse("always"));
        return policy;
    }
}
