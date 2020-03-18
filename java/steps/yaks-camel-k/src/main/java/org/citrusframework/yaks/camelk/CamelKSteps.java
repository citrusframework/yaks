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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.model.Integration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CamelKSteps {

    private static final int MAX_ATTEMPTS = System.getenv("YAKS_CAMELK_MAX_ATTEMPTS") != null ? Integer.valueOf(System.getenv("YAKS_CAMELK_MAX_ATTEMPTS")) : 150;
    private static final long DELAY_BETWEEN_ATTEMPTS = System.getenv("YAKS_CAMELK_DELAY_BETWEEN_ATTEMPTS") != null ? Long.valueOf(System.getenv("YAKS_CAMELK_DELAY_BETWEEN_ATTEMPTS")) : 2000;

    private KubernetesClient client;
    private ObjectMapper obj = new ObjectMapper();

	/** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CamelKSteps.class);

	@Given("^new integration with name ([a-z0-9_]+\\.[a-z0-9_]+) with configuration:$")
	public void createNewIntegration(String name, Map<String, String> configuration) throws IOException {
		if(configuration.get("source") == null) {
			throw new IllegalStateException("Specify 'source' parameter");
		}
		createIntegration(name, configuration.get("source"), configuration.get("dependencies"), configuration.get("traits"));

	}

	@Given("^new integration with name ([a-z0-9_]+\\.[a-z0-9_]+)$")
	public void createNewIntegration(String name, String source) throws IOException {
		createIntegration(name, source, null, null);

	}

    @Given("^integration ([a-z0-9-.]+) is running$")
    @Then("^integration ([a-z0-9-.]+) should be running$")
    public void shouldBeRunning(String integration) throws InterruptedException {
        if (getRunningIntegrationPod(integration, MAX_ATTEMPTS, DELAY_BETWEEN_ATTEMPTS) == null) {
            throw new IllegalStateException(String.format("integration %s not running after %d attempts", integration, MAX_ATTEMPTS));
        }
    }

    @Then("^integration ([a-z0-9-.]+) should print (.*)$")
    public void shouldPrint(String integration, String message) throws InterruptedException {
        Pod pod = getRunningIntegrationPod(integration, MAX_ATTEMPTS, DELAY_BETWEEN_ATTEMPTS);
        if (pod == null) {
            throw new IllegalStateException(String.format("integration %s not running after %d attempts", integration, MAX_ATTEMPTS));
        }
        if (!isIntegrationPodLogContaining(pod, message, MAX_ATTEMPTS, DELAY_BETWEEN_ATTEMPTS)) {
            throw new IllegalStateException(String.format("integration %s has not printed message %s after %d attempts", integration, message, MAX_ATTEMPTS));
        }
    }

    private boolean isIntegrationPodLogContaining(Pod pod, String message, int attempts, long delayBetweenAttempts) throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            String log = getIntegrationPodLogs(pod);
            if (log.contains(message)) {
                return true;
            }
            Thread.sleep(delayBetweenAttempts);
        }
        return false;
    }

    private String getIntegrationPodLogs(Pod pod) {
        PodResource<Pod, DoneablePod> podRes = client.pods()
                .inNamespace(namespace())
                .withName(pod.getMetadata().getName());

        String containerName = null;
        if (pod.getSpec() != null && pod.getSpec().getContainers() != null && pod.getSpec().getContainers().size() > 1) {
            containerName = pod.getSpec().getContainers().get(0).getName();
        }

        String logs;
        if (containerName != null) {
            logs = podRes.inContainer(containerName).getLog();
        } else {
            logs = podRes.getLog();
        }
        return logs;
    }

    private Pod getRunningIntegrationPod(String integration, int attempts, long delayBetweenAttempts) throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            Pod pod = getRunningIntegrationPod(integration);
            if (pod != null) {
                return pod;
            }
            Thread.sleep(delayBetweenAttempts);
        }
        return null;
    }

    private Pod getRunningIntegrationPod(String integration) {
        PodList pods = client().pods().inNamespace(namespace()).withLabel("camel.apache.org/integration", integration).list();
        if (pods.getItems().size() == 0) {
            return null;
        }
        for (Pod p : pods.getItems()) {
            if (p.getStatus() != null && "Running".equals(p.getStatus().getPhase())) {
                return p;
            }
        }
        return null;
    }

	private CustomResourceDefinitionContext getIntegrationCRD() {
		return new CustomResourceDefinitionContext.Builder()
				.withName(Integration.CRD_INTEGRATION_NAME)
				.withGroup(Integration.CRD_GROUP)
				.withVersion(Integration.CRD_VERSION)
				.withPlural("integrations")
				.withScope("Namespaced")
				.build();
	}

	private void createIntegration(String name, String source, String dependencies, String traits) {
		final Integration.Builder integrationBuilder = new Integration.Builder()
				.name(name)
				.source(source);

		if(dependencies != null && !dependencies.isEmpty()) {
			integrationBuilder.dependencies(Arrays.asList(dependencies.split(",")));
		}

		if (traits != null && !traits.isEmpty()) {
			final Map<String, Integration.TraitConfig> traitConfigMap = new HashMap<>();
			for(String t : traits.split(",")){
				//traitName.key=value
				if(!validateTraitFormat(t)) {
					throw new IllegalArgumentException("Trait" + t + "does not match format traitName.key=value");
				}
				final String[] trait = t.split("\\.",2);
				final String[] traitConfig = trait[1].split("=", 2);
				if(traitConfigMap.containsKey(trait[0])) {
					traitConfigMap.get(trait[0]).add(traitConfig[0], traitConfig[1]);
				} else {
					traitConfigMap.put(trait[0],  new Integration.TraitConfig(traitConfig[0], traitConfig[1]));
				}
			}
			integrationBuilder.traits(traitConfigMap);
		}

		final Integration i = integrationBuilder.build();

		final CustomResourceDefinitionContext crdContext = getIntegrationCRD();

		try {
			Map<String, Object> result = client().customResource(crdContext).createOrReplace(namespace(), obj.writeValueAsString(i));
			if(result.get("message") != null) {
				throw new IllegalStateException(result.get("message").toString());
			}
		} catch (IOException e) {
			throw new IllegalStateException("Can't create Integration JSON object", e);
		}
	}

	private boolean validateTraitFormat(String trait) {
		String patternString = "[A-Za-z-0-9]+\\.[A-Za-z-0-9]+=[A-Za-z-0-9]+";

		Pattern pattern = Pattern.compile(patternString);

		Matcher matcher = pattern.matcher(trait);
		return matcher.matches();
	}



    private String namespace() {
        String result = System.getenv("NAMESPACE");
		final File namespace = new File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
		if(result == null && namespace.exists()){
			try {
				result = new String(Files.readAllBytes(namespace.toPath()));
			} catch (IOException e) {
				LOG.warn("Can't read {}", namespace, e);
				return null;
			}
		}
		return result;
    }

    private KubernetesClient client() {
        if (this.client == null) {
            this.client = new DefaultKubernetesClient();
        }
        return this.client;
    }

}
