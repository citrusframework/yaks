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

package org.citrusframework.yaks.maven.extension.configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Christoph Deppisch
 */
public interface RepositoryLoader {

    /**
     * Load Maven repositories from configuration source.
     * @param logger
     * @param asPluginRepository
     * @return
     */
    List<Repository> load(Logger logger, boolean asPluginRepository) throws LifecycleExecutionException;

    /**
     * Construct repository instance from given url. Query parameters are translated to fields on the target repository.
     * @param id
     * @param url
     * @param logger
     * @return
     */
    default Repository build(String id, String url, Logger logger) throws LifecycleExecutionException {
        Repository repository = new Repository();

        repository.setId(id);

        try {
            URL configurationUrl = new URL(url);
            repository.setUrl(String.format("%s://%s%s", configurationUrl.getProtocol(), configurationUrl.getHost(), configurationUrl.getPath()));
        } catch (MalformedURLException e) {
            throw new LifecycleExecutionException("Failed to construct Maven repository model from given URL", e);
        }

        logger.info(String.format("Add Repository %s=%s", repository.getId(), repository.getUrl()));
        return repository;
    }
}
