package org.citrusframework.yaks.maven.extension.configuration;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.apache.maven.model.Dependency;
import org.assertj.core.api.Assertions;

/**
 * @author Christoph Deppisch
 */
public final class TestHelper {

    private TestHelper() {
        // prevent initialization
    }

    /**
     * Construct classpath resource path from given file name.
     * @param name
     * @return
     * @throws URISyntaxException
     */
    public static Path getClasspathResource(String name) throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(TestHelper.class.getClassLoader().getResource(name)).toURI());
    }

    /**
     * Verify that default mock dependencies are present in the given list od dependencies. This verification can be shared by multiple
     * tests that load the dependency list in different ways (e.g. via Json, Yaml, System properties, ...)
     * @param dependencyList
     */
    public static void verifyDependencies(List<Dependency> dependencyList) {
        Dependency foo = new Dependency();
        foo.setGroupId("org.foo");
        foo.setArtifactId("foo-artifact");
        foo.setVersion("1.0.0");
        foo.setType("jar");

        Dependency bar = new Dependency();
        bar.setGroupId("org.bar");
        bar.setArtifactId("bar-artifact");
        bar.setVersion("1.5.0");
        bar.setType("jar");

        Assertions.assertThat(dependencyList).hasSize(2);
        Assertions.assertThat(dependencyList).anyMatch(dependency -> dependency.toString().equals(foo.toString()));
        Assertions.assertThat(dependencyList).anyMatch(dependency -> dependency.toString().equals(bar.toString()));
    }
}
