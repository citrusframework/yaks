package org.apache.camel.k.testing.kubernetes;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;

public class KubernetesSteps {

    private static final int MAX_ATTEMPTS = 150;
    private static final long DELAY_BETWEEN_ATTEMPTS = 2000;

    private KubernetesClient client;

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

    private String namespace() {
        return System.getenv("NAMESPACE");
    }

    private KubernetesClient client() {
        if (this.client == null) {
            this.client = new DefaultKubernetesClient();
        }
        return this.client;
    }

}
