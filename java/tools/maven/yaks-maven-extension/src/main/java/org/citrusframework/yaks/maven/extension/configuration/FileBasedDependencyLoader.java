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
import java.util.Properties;

import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.json.JsonFileDependencyLoader;
import org.citrusframework.yaks.maven.extension.configuration.properties.PropertyFileDependencyLoader;
import org.citrusframework.yaks.maven.extension.configuration.yaml.YamlFileDependencyLoader;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * Add dependencies based on configuration living in a external file. Dependency loader delegates to multiple file based
 * loaders according to the file extension provided as configuration file (e.g. .yaml, .json, .properties).
 *
 * User can specify the configuration file to read via System property or environment variable.
 * @author Christoph Deppisch
 */
public class FileBasedDependencyLoader implements DependencyLoader {

    /** Map of file based config loaders where mapping key is the property file extension */
    private final Map<String, AbstractConfigFileDependencyLoader> fileConfigLoaders;

    public FileBasedDependencyLoader() {
        this.fileConfigLoaders = new HashMap<>();

        fileConfigLoaders.put("yaml", new YamlFileDependencyLoader());
        fileConfigLoaders.put("yml", new YamlFileDependencyLoader());
        fileConfigLoaders.put("json", new JsonFileDependencyLoader());
        fileConfigLoaders.put("properties", new PropertyFileDependencyLoader());
    }

    @Override
    public List<Dependency> load(Properties properties, Logger logger) throws LifecycleExecutionException {
        Path settingsFile = getSettingsFile();

        if (Files.exists(settingsFile)) {
            Optional<String> fileExtension = getFileNameExtension(settingsFile.getFileName().toString());
            return fileExtension.flatMap(this::getFileConfigLoader)
                                .orElse(new PropertyFileDependencyLoader())
                                .load(settingsFile, properties, logger);
        }

        return Collections.emptyList();
    }

    private Optional<AbstractConfigFileDependencyLoader> getFileConfigLoader(String fileExtension) {
        return Optional.ofNullable(fileConfigLoaders.get(fileExtension));
    }

    public static Path getSettingsFile() throws LifecycleExecutionException {
        String filePath = ExtensionSettings.getSettingsFilePath();

        if (filePath.startsWith("classpath:")) {
            try {
                URL resourceUrl = FileBasedDependencyLoader.class.getClassLoader().getResource(filePath.substring("classpath:".length()));
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
