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

import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
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

            Assert.assertEquals(4L, projectModel.getBuild().getTestResources().size());
            Assert.assertEquals("test-classes/org/citrusframework/yaks/feature", projectModel.getBuild().getTestResources().get(0).getTargetPath());
            Assert.assertEquals(1L, projectModel.getBuild().getTestResources().get(0).getIncludes().size());
            Assert.assertEquals("*.feature", projectModel.getBuild().getTestResources().get(0).getIncludes().get(0));
            Assert.assertEquals("test-classes/org/citrusframework/yaks/groovy", projectModel.getBuild().getTestResources().get(1).getTargetPath());
            Assert.assertEquals(1L, projectModel.getBuild().getTestResources().get(1).getIncludes().size());
            Assert.assertEquals("*it.groovy", projectModel.getBuild().getTestResources().get(1).getIncludes().get(0));
            Assert.assertEquals("test-classes/org/citrusframework/yaks/groovy", projectModel.getBuild().getTestResources().get(2).getTargetPath());
            Assert.assertEquals(1L, projectModel.getBuild().getTestResources().get(2).getIncludes().size());
            Assert.assertEquals("*test.groovy", projectModel.getBuild().getTestResources().get(2).getIncludes().get(0));

            Assert.assertEquals("test-classes", projectModel.getBuild().getTestResources().get(3).getTargetPath());
            Assert.assertEquals(3L, projectModel.getBuild().getTestResources().get(3).getExcludes().size());
            Assert.assertEquals("*.feature", projectModel.getBuild().getTestResources().get(3).getExcludes().get(0));
            Assert.assertEquals("*it.groovy", projectModel.getBuild().getTestResources().get(3).getExcludes().get(1));
            Assert.assertEquals("*test.groovy", projectModel.getBuild().getTestResources().get(3).getExcludes().get(2));
        } finally {
            System.setProperty(ExtensionSettings.TESTS_PATH_KEY, "");
        }
    }

}
