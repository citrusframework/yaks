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

package org.citrusframework.yaks.camelk.actions.integration;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * Test action verifies integration Pod running/stopped state and optionally waits for a log message to be present. Raises errors
 * when either the integration is not in expected state or the log message is not available. Both operations are automatically retried
 * for a given amount of attempts.
 *
 * @author Christoph Deppisch
 */
public class VerifyIntegrationAction extends AbstractCamelKAction {

    private final String integrationName;
    private final String logMessage;
    private final int maxAttempts;
    private final long delayBetweenAttempts;

    private final String phase;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyIntegrationAction(Builder builder) {
        super("verify-integration", builder);
        this.integrationName = builder.integrationName;
        this.phase = builder.phase;
        this.logMessage = builder.logMessage;
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
    }

    @Override
    public void doExecute(TestContext context) {
        String podName = context.replaceDynamicContentInString(integrationName);
        Pod pod = verifyIntegrationPod(podName, context.replaceDynamicContentInString(phase));

        if (logMessage != null) {
            verifyIntegrationLogs(pod, podName, context.replaceDynamicContentInString(logMessage));
        }
    }

    /**
     * Wait for integration pod to log given message.
     * @param pod
     * @param name
     * @param message
     */
    private void verifyIntegrationLogs(Pod pod, String name, String message) {
        for (int i = 0; i < maxAttempts; i++) {
            String log = getIntegrationPodLogs(pod);
            if (log.contains(message)) {
                LOG.info("Verified integration logs - All values OK!");
                return;
            }

            LOG.warn(String.format("Waiting for integration '%s' to log message - retry in %s ms", name, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for integration pod logs", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify integration '%s' - " +
                        "has not printed message '%s' after %d attempts", name, logMessage, maxAttempts)));
    }

    /**
     * Retrieve log messages from given pod.
     * @param pod
     * @return
     */
    private String getIntegrationPodLogs(Pod pod) {
        PodResource<Pod, DoneablePod> podRes = getKubernetesClient().pods()
                .inNamespace(CamelKSettings.getNamespace())
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

    /**
     * Wait for given pod to be in given state.
     * @param name
     * @param phase
     * @return
     */
    private Pod verifyIntegrationPod(String name, String phase) {
        for (int i = 0; i < maxAttempts; i++) {
            Pod pod = getIntegrationPod(name, phase);
            if (pod != null) {
                LOG.info(String.format("Verified integration pod '%s' state '%s'!", name, phase));
                return pod;
            }

            LOG.warn(String.format("Waiting for integration '%s' in state '%s'- retry in %s ms", name, phase, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for integration pod state", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify integration '%s' - " +
                        "is not in state '%s' after %d attempts", name, phase, maxAttempts)));
    }

    /**
     * Retrieve pod given state.
     * @param integration
     * @param phase
     * @return
     */
    private Pod getIntegrationPod(final String integration, final String phase) {
        PodList pods = getKubernetesClient().pods()
                .inNamespace(CamelKSettings.getNamespace())
                .withLabel(CamelKSettings.INTEGRATION_LABEL, integration)
                .list();

        return pods.getItems().stream()
                .filter(pod -> KubernetesSupport.verifyPodStatus(pod, phase))
                .findFirst()
                .orElse(null);
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<VerifyIntegrationAction, Builder> {

        private String integrationName;
        private String logMessage;

        private int maxAttempts = CamelKSettings.getMaxAttempts();
        private long delayBetweenAttempts = CamelKSettings.getDelayBetweenAttempts();

        private String phase = "Running";

        public Builder isRunning() {
            this.phase = "Running";
            return this;
        }

        public Builder isStopped() {
            this.phase = "Stopped";
            return this;
        }

        public Builder integrationName(String integrationName) {
            this.integrationName = integrationName;
            return this;
        }

        public Builder waitForLogMessage(String logMessage) {
            this.logMessage = logMessage;
            return this;
        }

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder delayBetweenAttempts(long delayBetweenAttempts) {
            this.delayBetweenAttempts = delayBetweenAttempts;
            return this;
        }

        @Override
        public VerifyIntegrationAction build() {
            return new VerifyIntegrationAction(this);
        }
    }
}
