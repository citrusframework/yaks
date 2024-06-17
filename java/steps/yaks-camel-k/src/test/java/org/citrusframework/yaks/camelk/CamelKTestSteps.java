/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.camelk;

import java.util.Collections;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.camel.v1.Integration;
import org.apache.camel.v1.IntegrationBuilder;
import org.citrusframework.Citrus;
import org.citrusframework.annotations.CitrusFramework;
import org.citrusframework.yaks.camelk.model.IntegrationList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * @author Christoph Deppisch
 */
public class CamelKTestSteps {

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^Camel K integration pod ([a-z0-9-]+)$")
    public void createIntegrationPod(String integrationName) {
        createIntegrationPod(integrationName, "Running");
    }

    @Given("^Camel K integration pod ([a-z0-9-]+) in phase (Running|Stopped)$")
    public void createIntegrationPod(String integrationName, String phase) {
        Integration integration = new IntegrationBuilder()
                .withNewMetadata()
                    .withName(integrationName)
                .endMetadata()
                .withNewStatus()
                    .withPhase(phase)
                .endStatus()
                .build();

        k8sClient.resources(Integration.class, IntegrationList.class).inNamespace(CamelKSettings.getNamespace()).resource(integration).create();

        Pod pod = new PodBuilder()
                .withNewMetadata()
                    .withName(integrationName)
                    .withNamespace(CamelKSettings.getNamespace())
                    .withLabels(Collections.singletonMap(CamelKSettings.INTEGRATION_LABEL, integrationName))
                .endMetadata()
                .withNewStatus()
                    .withPhase(phase)
                .endStatus()
                .build();

        k8sClient.pods().inNamespace(CamelKSettings.getNamespace()).resource(pod).create();
    }
}
