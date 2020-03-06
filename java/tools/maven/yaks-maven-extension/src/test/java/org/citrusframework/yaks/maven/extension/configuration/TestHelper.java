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
