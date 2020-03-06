package org.citrusframework.yaks.maven.extension.configuration.cucumber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.citrusframework.yaks.maven.extension.ExtensionSettings;
import org.citrusframework.yaks.maven.extension.configuration.DependencyLoader;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * Loader reviews all Cucumber feature files and adds additional dependencies based on tag information on scenarios. Users
 * can add a special tag to the feature file in order to load the dependency through this loader.
 *
 * Users can add Maven coordinates using a special tag information on the BDD feature files like "@require('org.foo:foo-artifact:1.0.0')".
 * @author Christoph Deppisch
 */
public class FeatureTagsDependencyLoader implements DependencyLoader {

    private static final Pattern TAG_PATTERN = Pattern.compile("^(?:@require\\('?(?<coordinate>[^']+?)'?\\))$");

    @Override
    public List<Dependency> load(Properties properties, Logger logger) throws LifecycleExecutionException {
        List<Dependency> dependencyList = new ArrayList<>();

        loadFromFileSystem(dependencyList, properties, logger);
        loadFromClasspath(dependencyList, properties, logger);

        return dependencyList;
    }

    /**
     * Visit BDD feature files in classpath and load dependency tags in those feature files. This method only supports first level
     * resource directory in classpath.
     * @param dependencyList
     * @param properties
     * @param logger
     * @return
     */
    private void loadFromClasspath(List<Dependency> dependencyList, Properties properties, Logger logger) throws LifecycleExecutionException {
        try {
            Files.list(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("")).toURI()))
                    .filter(file -> file.getFileName().toString().endsWith(ExtensionSettings.FEATURE_FILE_EXTENSION))
                    .forEach(file -> {
                        try {
                            dependencyList.addAll(loadDependencyTags(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), properties, logger));
                        } catch (IOException e) {
                            logger.warn("Failed to read BDD feature", e);
                        }
                    });
        } catch (IOException | URISyntaxException e) {
            throw new LifecycleExecutionException("Failed to retrieve BDD feature files in classpath", e);
        }
    }

    /**
     * Visit all files in provided tests path (if any) and load dependency tags in feature files.
     * @param dependencyList
     * @param properties
     * @param logger
     * @return
     * @throws LifecycleExecutionException
     */
    private void loadFromFileSystem(List<Dependency> dependencyList, Properties properties, Logger logger) throws LifecycleExecutionException {
        String testsPath = ExtensionSettings.getMountedTestsPath();

        if (testsPath.length() > 0) {
            try {
                Files.walk(Paths.get(testsPath))
                        .filter(file -> file.getFileName().toString().endsWith(ExtensionSettings.FEATURE_FILE_EXTENSION))
                        .forEach(file -> {
                            try {
                                dependencyList.addAll(loadDependencyTags(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), properties, logger));
                            } catch (IOException e) {
                                logger.warn("Failed to read BDD feature", e);
                            }
                        });
            } catch (IOException e) {
                throw new LifecycleExecutionException(String.format("Failed to retrieve BDD feature files in tests path '%s'", testsPath), e);
            }
        }
    }

    /**
     * Load all dependencies specified through BDD tag information in given feature file content.
     * @param feature
     * @param properties
     * @param logger
     * @return
     */
    private List<Dependency> loadDependencyTags(String feature, Properties properties, Logger logger) {
        return new BufferedReader(new StringReader(feature))
                .lines()
                .map(TAG_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> {
                    try {
                        return build(matcher.group("coordinate"), properties, logger);
                    } catch (LifecycleExecutionException e) {
                        logger.error("Failed to read dependency tag information", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
