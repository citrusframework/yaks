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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.citrusframework.yaks.maven.extension.configuration.FileBasedDependencyLoader;
import org.citrusframework.yaks.maven.extension.configuration.FileBasedLoggingConfigurationLoader;
import org.citrusframework.yaks.maven.extension.configuration.LoggingConfigurationLoader;
import org.citrusframework.yaks.maven.extension.configuration.cucumber.FeatureTagsDependencyLoader;
import org.citrusframework.yaks.maven.extension.configuration.env.EnvironmentSettingDependencyLoader;
import org.citrusframework.yaks.maven.extension.configuration.env.EnvironmentSettingLoggingConfigurationLoader;
import org.citrusframework.yaks.maven.extension.configuration.properties.SystemPropertyDependencyLoader;
import org.citrusframework.yaks.maven.extension.configuration.properties.SystemPropertyLoggingConfigurationLoader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Project listener dynamically changes the Maven project model prior to building the project. This way the extension can
 * add configuration as if it was added to the pom.xml.
 *
 * @author Christoph Deppisch
 */
@Component( role = ProjectExecutionListener.class )
public class ProjectModelEnricher implements ProjectExecutionListener {

    @Requirement
    Logger logger;

    /** Set of supported source test types */
    private static final String[] SOURCE_TYPES = new String[] {".feature", "it.groovy", "test.groovy", "IT.xml", "Test.xml", "it.xml", "test.xml", "it.yaml", "test.yaml"};

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
        Model projectModel = projectExecutionEvent.getProject().getModel();
        injectProjectDependencies(projectModel);
        injectTestResources(projectModel);
        injectSecrets(projectModel);
        loadLoggingConfiguration(projectExecutionEvent.getProject());
    }

    /**
     * Dynamically add test resource directory pointing to mounted test directory. Mounted directory usually gets added
     * as volume mount and holds tests to execute in this project.
     * @param projectModel
     */
    private void injectTestResources(Model projectModel) {
        if (ExtensionSettings.hasMountedTests()) {
            logger.info("Add mounted test resources from directory: " + ExtensionSettings.getMountedTestsPath());

            Arrays.stream(SOURCE_TYPES).forEach(sourceType -> projectModel.getBuild().getTestResources().add(
                    getMountedTests(projectModel, sourceType)));

            Resource mountedResources = new Resource();
            mountedResources.setDirectory(ExtensionSettings.getMountedTestsPath() + "/..data");
            mountedResources.setTargetPath(projectModel.getBuild().getTestOutputDirectory());
            mountedResources.setFiltering(false);
            mountedResources.setExcludes(Arrays.stream(SOURCE_TYPES).map(sourceType -> String.format("*%s", sourceType)).collect(Collectors.toList()));
            projectModel.getBuild().getTestResources().add(mountedResources);
        }
    }

    /**
     * Dynamically adds Maven test resources from container volume mount to the Maven test output directory.
     * @param projectModel
     * @param type
     * @return
     */
    private static Resource getMountedTests(Model projectModel, String type) {
        Resource mountedTests = new Resource();
        mountedTests.setDirectory(ExtensionSettings.getMountedTestsPath() + "/..data");
        mountedTests.setTargetPath(String.format("%s/org/citrusframework/yaks/%s", projectModel.getBuild().getTestOutputDirectory(), type.substring(type.lastIndexOf(".") + 1)));
        mountedTests.setFiltering(false);
        mountedTests.setIncludes(Collections.singletonList(String.format("*%s", type)));

        return mountedTests;
    }

    /**
     * Dynamically add test resource directory pointing to mounted secrets directory. Mounted directory usually gets added
     * as volume mount and holds property files holding secrets to load in this project.
     * @param projectModel
     */
    private void injectSecrets(Model projectModel) {
        if (ExtensionSettings.hasMountedSecrets()) {
            logger.info("Add mounted secrets from directory: " + ExtensionSettings.getMountedSecretsPath());

            Resource mountedSecrets = new Resource();
            mountedSecrets.setDirectory(ExtensionSettings.getMountedSecretsPath() + "/..data");
            mountedSecrets.setTargetPath(projectModel.getBuild().getTestOutputDirectory() + "/secrets");
            mountedSecrets.setFiltering(false);
            projectModel.getBuild().getTestResources().add(mountedSecrets);
        }
    }

    /**
     * Dynamically add project dependencies based on different configuration sources such as environment variables,
     * system properties and configuration files.
     * @param projectModel
     * @throws LifecycleExecutionException
     */
    private void injectProjectDependencies(Model projectModel) throws LifecycleExecutionException {
        logger.info("Add dynamic project dependencies ...");
        List<Dependency> dependencyList = projectModel.getDependencies();
        dependencyList.addAll(new FileBasedDependencyLoader().load(projectModel.getProperties(), logger));
        dependencyList.addAll(new SystemPropertyDependencyLoader().load(projectModel.getProperties(), logger));
        dependencyList.addAll(new EnvironmentSettingDependencyLoader().load(projectModel.getProperties(), logger));
        dependencyList.addAll(new FeatureTagsDependencyLoader().load(projectModel.getProperties(), logger));
    }

    /**
     * Dynamically load logger configuration based on different configuration sources such as environment variables,
     * system properties and configuration files.
     * @throws LifecycleExecutionException
     * @param project
     */
    private void loadLoggingConfiguration(MavenProject project) throws LifecycleExecutionException {
        logger.info("Load logger configuration ...");
        final ConfigurationBuilder<BuiltConfiguration> builder = LoggingConfigurationLoader.newConfigurationBuilder();

        Level rootLevel = Level.ERROR;
        rootLevel = new FileBasedLoggingConfigurationLoader().load(builder, logger).orElse(rootLevel);
        rootLevel = new SystemPropertyLoggingConfigurationLoader().load(builder, logger).orElse(rootLevel);
        rootLevel = new EnvironmentSettingLoggingConfigurationLoader().load(builder, logger).orElse(rootLevel);

        builder.add(builder.newRootLogger(rootLevel).add(builder.newAppenderRef("STDOUT")));

        try {
            ByteArrayOutputStream configuration = new ByteArrayOutputStream();
            builder.writeXmlConfiguration(configuration);
            Files.createDirectories(Paths.get(project.getBasedir().toURI()).resolve("src/test/resources"));
            Files.write(Paths.get(project.getBasedir().toURI()).resolve("src/test/resources/log4j2-test.xml"),
                        configuration.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new LifecycleExecutionException("Failed to write Log4j2 configuration file", e);
        }
    }

    @Override
    public void beforeProjectLifecycleExecution(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
        // do nothing
    }

    @Override
    public void afterProjectExecutionSuccess(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
        // do nothing
    }

    @Override
    public void afterProjectExecutionFailure(ProjectExecutionEvent projectExecutionEvent) {
        // do nothing
    }
}
