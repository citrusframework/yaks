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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Repository;
import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.json.JsonFileRepositoryLoader;
import org.citrusframework.yaks.maven.extension.configuration.properties.PropertyFileRepositoryLoader;
import org.citrusframework.yaks.maven.extension.configuration.yaml.YamlFileRepositoryLoader;
import org.codehaus.plexus.logging.Logger;

/**
 * Add repositories based on configuration living in a external file. Repository loader delegates to multiple file based
 * loaders according to the file extension provided as configuration file (e.g. .yaml, .json, .properties).
 *
 * User can specify the configuration file to read via System property or environment variable.
 * @author Christoph Deppisch
 */
public class FileBasedRepositoryLoader implements RepositoryLoader {

    /** Map of file based config loaders where mapping key is the property file extension */
    private final Map<String, AbstractConfigFileRepositoryLoader> fileConfigLoaders;

    public FileBasedRepositoryLoader() {
        this.fileConfigLoaders = new HashMap<>();

        fileConfigLoaders.put("yaml", new YamlFileRepositoryLoader());
        fileConfigLoaders.put("yml", new YamlFileRepositoryLoader());
        fileConfigLoaders.put("json", new JsonFileRepositoryLoader());
        fileConfigLoaders.put("properties", new PropertyFileRepositoryLoader());
    }

    @Override
    public List<Repository> load(Logger logger, boolean asPluginRepository) throws LifecycleExecutionException {
        Path settingsFile = getSettingsFile();

        if (Files.exists(settingsFile)) {
            Optional<String> fileExtension = getFileNameExtension(settingsFile.getFileName().toString());
            return fileExtension.flatMap(this::getFileConfigLoader)
                                .orElse(new PropertyFileRepositoryLoader())
                                .load(settingsFile, logger, asPluginRepository);
        }

        return Collections.emptyList();
    }

    private Optional<AbstractConfigFileRepositoryLoader> getFileConfigLoader(String fileExtension) {
        return Optional.ofNullable(fileConfigLoaders.get(fileExtension));
    }

    public static Path getSettingsFile() throws LifecycleExecutionException {
        String filePath = ExtensionSettings.getSettingsFilePath();

        if (filePath.startsWith("classpath:")) {
            try {
                URL resourceUrl = FileBasedRepositoryLoader.class.getClassLoader().getResource(filePath.substring("classpath:".length()));
                if (resourceUrl != null) {
                    return Paths.get(resourceUrl.toURI());
                }
            } catch (URISyntaxException e) {
                throw new LifecycleExecutionException("Unable to locate properties file in classpath", e);
            }
        } else if (filePath.startsWith("file:")) {
            return Paths.get(filePath.substring("file:".length()));
        }

        return Paths.get(filePath);
    }

    public static Optional<String> getFileNameExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
