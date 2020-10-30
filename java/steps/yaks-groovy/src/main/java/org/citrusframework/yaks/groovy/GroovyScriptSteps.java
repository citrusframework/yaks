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

package org.citrusframework.yaks.groovy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.common.InitializingPhase;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.citrusframework.yaks.groovy.dsl.ConfigurationScript;
import org.citrusframework.yaks.groovy.dsl.actions.ActionScript;
import org.citrusframework.yaks.groovy.dsl.endpoints.EndpointConfigurationScript;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.core.io.Resource;

/**
 * @author Christoph Deppisch
 */
public class GroovyScriptSteps {

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestContext context;

    @CitrusResource
    private TestCaseRunner runner;

    private Map<String, ActionScript> scripts;

    @Before
    public void before(Scenario scenario) {
        this.scripts = new HashMap<>();
    }

    @Given("^(?:create|new) configuration$")
    public void createConfiguration(String config) {
        GroovyShellUtils.run(new ImportCustomizer(), new ConfigurationScript(citrus), config);
    }

    @Given("^load configuration ([^\"\\s]+)\\.groovy$")
    public void loadConfiguration(String filePath) throws IOException {
        Resource scriptFile = FileUtils.getFileResource(filePath + ".groovy");
        String script = FileUtils.readToString(scriptFile);
        createConfiguration(script);
    }

    @Given("^(?:create|new) endpoint ([^\"\\s]+)\\.groovy$")
    public void createEndpoint(String name, String configurationScript) {
        Endpoint endpoint = new EndpointConfigurationScript(configurationScript).get();

        if (endpoint instanceof InitializingPhase) {
            ((InitializingPhase) endpoint).initialize();
        }

        citrus.getCitrusContext().bind(name, endpoint);
    }

    @Given("^load endpoint ([^\"\\s]+)\\.groovy$")
    public void loadEndpoint(String filePath) throws IOException {
        Resource scriptFile = FileUtils.getFileResource(filePath + ".groovy");
        String script = FileUtils.readToString(scriptFile);
        createEndpoint(scriptFile.getFilename(), script);
    }

    @Given("^(?:create|new) actions ([^\"\\s]+)\\.groovy$")
    public void createActionScript(String scriptName, String code) {
        scripts.put(scriptName, new ActionScript(code));
    }

    @Given("^load actions ([^\"\\s]+)\\.groovy$")
    public void loadActionScript(String filePath) throws IOException {
        Resource scriptFile = FileUtils.getFileResource(filePath + ".groovy");
        String script = FileUtils.readToString(scriptFile);
        final String fileName = scriptFile.getFilename();
        final String baseName = Optional.ofNullable(fileName)
                .map(f -> f.lastIndexOf("."))
                .filter(index -> index >= 0)
                .map(index -> fileName.substring(0, index))
                .orElse(fileName);
        createActionScript(baseName, script);
    }

    @Then("^(?:apply|verify) ([^\"\\s]+)\\.groovy$")
    public void runScript(String scriptName) {
        if (!scripts.containsKey(scriptName)) {
            try {
                loadActionScript(scriptName + ".groovy");
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load/get action script for path/name %s.groovy", scriptName), e);
            }
        }

        Optional.ofNullable(scripts.get(scriptName))
                .orElseThrow(() -> new CitrusRuntimeException(String.format("Unable to find action script %s.groovy", scriptName)))
                .execute(runner);
    }
}
