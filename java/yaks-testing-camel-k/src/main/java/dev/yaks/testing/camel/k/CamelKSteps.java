package dev.yaks.testing.camel.k;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;


public class CamelKSteps {

	private static final String CRD_GROUP = "camel.apache.org";
	private static final String CRD_VERSION = "v1alpha1";
	private static final String CRD_INTEGRATION_NAME = "integrations.camel.apache.org";

    private static final int MAX_ATTEMPTS = 150;
    private static final long DELAY_BETWEEN_ATTEMPTS = 2000;

	private static final String INTEGRATION_TEMPLATE =
					"{\"apiVersion\": \"" + CRD_GROUP + "/" + CRD_VERSION + "\"," +
					"\"kind\": \"Integration\"," +
					"\"metadata\": {\"name\": \"${integrationName}\"}," +
					"\"spec\": {" +
					"\"dependencies\": [${dependencies}]," +
					"\"sources\": [" +
					"{" +
					"\"content\": \"${source}\"," +
					"\"name\": \"${fileName}\"" +
					"}]}" +
					"}";

    private KubernetesClient client;

	/** Logger */
	private static final Logger LOG = LoggerFactory.getLogger(CamelKSteps.class);

	@Given("^new integration with name ([a-z0-9_]+\\.[a-z0-9_]+)$")
	public void createNewIntegration(String name, String integration) throws IOException {
		createNewIntegration(name, "", integration);
	}

	@Given("^new integration with name ([a-z0-9_]+\\.[a-z0-9_]+) and dependencies (([A-Za-z]+:[a-z.]+:[A-Za-z0-9._-]+(:[A-Za-z0-9._-]+)?,?)+)$")
	public void createNewIntegration(String name, String dependencies, String integration) throws IOException {
		final String rawJsonIntegration = createJsonIntegration(name, integration, dependencies.split(","));
        final CustomResourceDefinitionContext crdContext = getIntegrationCRD();

		Map<String, Object> result = client().customResource(crdContext).createOrReplace(namespace(), rawJsonIntegration);
		if(result.get("message") != null) {
			throw new IllegalStateException(result.get("message").toString());
		}
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
				.withName(CRD_INTEGRATION_NAME)
				.withGroup(CRD_GROUP)
				.withVersion(CRD_VERSION)
				.withPlural("integrations")
				.withScope("Namespaced")
				.build();
	}

	private String createJsonIntegration(String name, String source, String... dependencies) {
		final Map<String, String> values = new HashMap<>();
		values.put("integrationName", name.substring(0, name.indexOf(".")));
        values.put("source", StringEscapeUtils.escapeJson(source));
		values.put("fileName", name);

		final String joined = Arrays.stream(dependencies)
				.filter(d -> !d.isEmpty())
				.map(d -> StringEscapeUtils.escapeJson(d))
				.map(d -> String.format("\"%s\"", d))
				.collect(Collectors.joining(","));
        values.put("dependencies", joined);

		final StringSubstitutor sub = new StringSubstitutor(values);
		return sub.replace(INTEGRATION_TEMPLATE);
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
