package dev.yaks.maven.extension;

import java.util.List;

import dev.yaks.maven.extension.configuration.FileBasedDependencyLoader;
import dev.yaks.maven.extension.configuration.cucumber.FeatureTagsDependencyLoader;
import dev.yaks.maven.extension.configuration.env.EnvironmentSettingDependencyLoader;
import dev.yaks.maven.extension.configuration.properties.SystemPropertyDependencyLoader;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
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
public class ResourceInjectionListener implements ProjectExecutionListener {

    @Requirement
    private Logger logger;

    @Override
    public void beforeProjectExecution(ProjectExecutionEvent projectExecutionEvent) throws LifecycleExecutionException {
        Model projectModel = projectExecutionEvent.getProject().getModel();
        injectProjectDependencies(projectModel);
        injectTestResources(projectModel);
    }

    /**
     * Dynamically add test resource directory pointing to mounted test directory. Mounted directory usually gets added
     * as volume mount and holds tests to execute in this project.
     * @param projectModel
     */
    private void injectTestResources(Model projectModel) {
        if (ExtensionSettings.hasMountedTests()) {
            Resource mountedTests = new Resource();
            mountedTests.setDirectory(ExtensionSettings.getMountedTestsPath() + "/..data");
            mountedTests.setTargetPath(projectModel.getBuild().getTestOutputDirectory() + "/dev/yaks/testing");
            mountedTests.setFiltering(false);
            projectModel.getBuild().getTestResources().add(mountedTests);

            logger.info("Add mounted test resources in directory: " + ExtensionSettings.getMountedTestsPath());
        }
    }

    /**
     * Dynamically add project dependencies based on different configuration sources such as environment variables,
     * system properties configuration files.
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
