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

package org.citrusframework.yaks.camelk;

import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletSpec;
import org.springframework.util.StringUtils;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.camelk.actions.CamelKActionBuilder.camelk;


public class KameletSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    private Kamelet.Builder builder;
    private KameletSpec.Definition definition;

    private boolean autoRemoveResources = CamelKSettings.isAutoRemoveResources();

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = CamelKSupport.getKubernetesClient(citrus);
        }

        builder = new Kamelet.Builder();
        definition = new KameletSpec.Definition();
    }

    @Given("^Disable auto removal of Kamelet resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

    @Given("^Enable auto removal of Kamelet resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;
    }

	@Given("^Kamelet type (in|out|error)(?:=| is )\"(.+)\"$")
	public void addType(String slot, String mediaType) {
        builder.addType(slot, mediaType);
	}

    @Given("^Kamelet title \"(.+)\"$")
	public void setTitle(String title) {
        definition.setTitle(title);
	}

    @Given("^Kamelet source ([a-z0-9-]+).([a-z0-9-]+)$")
	public void setSource(String name, String language, String content) {
        builder.source(name, language, content);
	}

    @Given("^Kamelet flow$")
	public void setFlow(String flow) {
        builder.flow(flow);
	}

    @Given("^Kamelet property definition$")
	public void addPropertyDefinition(Map<String, Object> propertyConfiguration) {
        if (!propertyConfiguration.containsKey("name")) {
            throw new CitrusRuntimeException("Missing property name in configuration. " +
                    "Please add the property name to the property definition");
        }

        String propertyName = propertyConfiguration.get("name").toString();
        String type = propertyConfiguration.getOrDefault("type", "string").toString();
        String title = propertyConfiguration.getOrDefault("title", StringUtils.capitalize(propertyName)).toString();
        Object defaultValue = propertyConfiguration.get("default");
        Object example = propertyConfiguration.get("example");
        String required = propertyConfiguration.getOrDefault("required", Boolean.FALSE).toString();

        if (Boolean.parseBoolean(required)) {
            definition.getRequired().add(propertyName);
        }

        definition.getProperties().put(propertyName,
                new KameletSpec.Definition.PropertyConfig(title, type, defaultValue, example));
	}

    @Given("^create Kamelet ([a-z0-9-]+)$")
	public void createNewKamelet(String name) {
        builder.name(name);

        if (definition.getTitle() == null || definition.getTitle().isEmpty()) {
            definition.setTitle(StringUtils.capitalize(name));
        }

        builder.definition(definition);

        runner.run(camelk()
                    .client(k8sClient)
                    .createKamelet(name)
                    .fromBuilder(builder));

        builder = new Kamelet.Builder();

        if (autoRemoveResources) {
            runner.then(doFinally()
                    .actions(camelk().client(k8sClient).deleteKamelet(name)));
        }
	}

	@Given("^create Kamelet ([a-z0-9-]+) with flow$")
	public void createNewKameletWithFlow(String name, String flow) {
        builder.flow(flow);
        createNewKamelet(name);
	}

    @Given("^remove Kamelet$")
	public void deleteKamelet(String name) {
        runner.run(camelk()
                    .client(k8sClient)
                    .deleteKamelet(name));
	}

    @Given("^Kamelet ([a-z0-9-]+) is available$")
    @Then("^Kamelet ([a-z0-9-]+) should be available$")
    public void shouldBeAvailable(String name) {
        runner.run(camelk()
                .client(k8sClient)
                .verifyKamelet(name)
                .isAvailable());
    }
}
