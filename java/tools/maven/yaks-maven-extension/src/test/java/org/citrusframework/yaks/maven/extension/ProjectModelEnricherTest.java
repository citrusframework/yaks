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

import java.io.File;
import java.util.List;

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * @author Christoph Deppisch
 */
public class ProjectModelEnricherTest {

    private final ProjectModelEnricher modelEnricher = new ProjectModelEnricher();
    private Model projectModel;

    @Mock
    private ProjectExecutionEvent executionEvent;

    @Mock
    private MavenProject mavenProject;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        projectModel = new Model();
        projectModel.setBuild(new Build());
        projectModel.getBuild().setTestOutputDirectory("test-classes");

        when(executionEvent.getProject()).thenReturn(mavenProject);
        when(mavenProject.getModel()).thenReturn(projectModel);

        when(mavenProject.getBasedir()).thenReturn(new File("target"));

        modelEnricher.logger = new ConsoleLogger();
    }

    @Test
    public void shouldAddTestResources() throws LifecycleExecutionException {
        try {
            System.setProperty(ExtensionSettings.TESTS_PATH_KEY, "target");

            modelEnricher.beforeProjectExecution(executionEvent);

            List<Resource> testResources = projectModel.getBuild().getTestResources();
            Assert.assertEquals(10L, testResources.size());

            Resource gherkinResource = testResources.get(0);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/feature", gherkinResource.getTargetPath());
            Assert.assertEquals(1L, gherkinResource.getIncludes().size());
            Assert.assertEquals("*.feature", gherkinResource.getIncludes().get(0));

            Resource groovyResource = testResources.get(1);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/groovy", groovyResource.getTargetPath());
            Assert.assertEquals(1L, groovyResource.getIncludes().size());
            Assert.assertEquals("*it.groovy", groovyResource.getIncludes().get(0));
            groovyResource = testResources.get(2);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/groovy", groovyResource.getTargetPath());
            Assert.assertEquals(1L, groovyResource.getIncludes().size());
            Assert.assertEquals("*test.groovy", groovyResource.getIncludes().get(0));

            Resource xmlResource = testResources.get(3);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/xml", xmlResource.getTargetPath());
            Assert.assertEquals(1L, xmlResource.getIncludes().size());
            Assert.assertEquals("*IT.xml", xmlResource.getIncludes().get(0));
            xmlResource = testResources.get(4);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/xml", xmlResource.getTargetPath());
            Assert.assertEquals(1L, xmlResource.getIncludes().size());
            Assert.assertEquals("*Test.xml", xmlResource.getIncludes().get(0));

            xmlResource = testResources.get(5);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/xml", xmlResource.getTargetPath());
            Assert.assertEquals(1L, xmlResource.getIncludes().size());
            Assert.assertEquals("*it.xml", xmlResource.getIncludes().get(0));
            xmlResource = testResources.get(6);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/xml", xmlResource.getTargetPath());
            Assert.assertEquals(1L, xmlResource.getIncludes().size());
            Assert.assertEquals("*test.xml", xmlResource.getIncludes().get(0));

            Resource yamlResource = testResources.get(7);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/yaml", yamlResource.getTargetPath());
            Assert.assertEquals(1L, yamlResource.getIncludes().size());
            Assert.assertEquals("*it.yaml", yamlResource.getIncludes().get(0));
            yamlResource = testResources.get(8);
            Assert.assertEquals("test-classes/org/citrusframework/yaks/yaml", yamlResource.getTargetPath());
            Assert.assertEquals(1L, yamlResource.getIncludes().size());
            Assert.assertEquals("*test.yaml", yamlResource.getIncludes().get(0));

            Resource otherResource = testResources.get(9);
            Assert.assertEquals("test-classes", otherResource.getTargetPath());
            Assert.assertEquals(9L, otherResource.getExcludes().size());
            Assert.assertEquals("*.feature", otherResource.getExcludes().get(0));
            Assert.assertEquals("*it.groovy", otherResource.getExcludes().get(1));
            Assert.assertEquals("*test.groovy", otherResource.getExcludes().get(2));
            Assert.assertEquals("*IT.xml", otherResource.getExcludes().get(3));
            Assert.assertEquals("*Test.xml", otherResource.getExcludes().get(4));
            Assert.assertEquals("*it.xml", otherResource.getExcludes().get(5));
            Assert.assertEquals("*test.xml", otherResource.getExcludes().get(6));
            Assert.assertEquals("*it.yaml", otherResource.getExcludes().get(7));
            Assert.assertEquals("*test.yaml", otherResource.getExcludes().get(8));
        } finally {
            System.setProperty(ExtensionSettings.TESTS_PATH_KEY, "");
        }
    }

}
