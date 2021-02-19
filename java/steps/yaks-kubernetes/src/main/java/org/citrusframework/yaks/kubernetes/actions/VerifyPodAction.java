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

package org.citrusframework.yaks.kubernetes.actions;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.citrusframework.yaks.kubernetes.KubernetesSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * Test action verifies pod phase in running/stopped state and optionally waits for a log message to be present. Raises errors
 * when either the pod is not in expected state or the log message is not available. Both operations are automatically retried
 * for a given amount of attempts.
 *
 * @author Christoph Deppisch
 */
public class VerifyPodAction extends AbstractKubernetesAction {

    private final String podName;
    private final String labelExpression;
    private final String logMessage;
    private final int maxAttempts;
    private final long delayBetweenAttempts;

    private final String phase;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyPodAction(Builder builder) {
        super("verify-pod-status", builder);
        this.podName = builder.podName;
        this.labelExpression = builder.labelExpression;
        this.phase = builder.phase;
        this.logMessage = builder.logMessage;
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
    }

    @Override
    public void doExecute(TestContext context) {
        String resolvedPodName = context.replaceDynamicContentInString(podName);
        String resolvedLabelExpression = context.replaceDynamicContentInString(labelExpression);
        Pod pod = verifyPod(resolvedPodName, resolvedLabelExpression,
                context.replaceDynamicContentInString(phase));

        if (logMessage != null) {
            verifyPodLogs(pod, getNameOrLabel(resolvedPodName, resolvedLabelExpression), context.replaceDynamicContentInString(logMessage));
        }
    }

    /**
     * Wait for pod to log given message.
     * @param pod
     * @param nameOrLabel
     * @param message
     */
    private void verifyPodLogs(Pod pod, String nameOrLabel, String message) {
        for (int i = 0; i < maxAttempts; i++) {
            String log = getPodLogs(pod);
            if (log.contains(message)) {
                LOG.info("Verified pod logs - All values OK!");
                return;
            }

            LOG.warn(String.format("Waiting for pod '%s' to log message - retry in %s ms", nameOrLabel, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for pod logs", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify pod '%s' - " +
                        "has not printed message '%s' after %d attempts", nameOrLabel, logMessage, maxAttempts)));
    }

    /**
     * Retrieve log messages from given pod.
     * @param pod
     * @return
     */
    private String getPodLogs(Pod pod) {
        PodResource<Pod> podRes = getKubernetesClient().pods()
                .inNamespace(KubernetesSettings.getNamespace())
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
     * @param labelExpression
     * @param phase
     * @return
     */
    private Pod verifyPod(String name, String labelExpression, String phase) {
        for (int i = 0; i < maxAttempts; i++) {
            Pod pod;
            if (name != null && !name.isEmpty()) {
                pod = getPod(name, phase);
            } else {
                pod = getPodFromLabel(labelExpression, phase);
            }

            if (pod != null) {
                LOG.info(String.format("Verified pod '%s' state '%s'!", getNameOrLabel(name, labelExpression), phase));
                return pod;
            }

            LOG.warn(String.format("Waiting for pod '%s' in state '%s' - retry in %s ms",
                    getNameOrLabel(name, labelExpression), phase, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for pod state", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify pod '%s' - " +
                        "is not in state '%s' after %d attempts", getNameOrLabel(name, labelExpression), phase, maxAttempts)));
    }

    /**
     * Retrieve pod given state.
     * @param name
     * @param phase
     * @return
     */
    private Pod getPod(String name, String phase) {
        Pod pod = getKubernetesClient().pods()
                .inNamespace(KubernetesSettings.getNamespace())
                .withName(name)
                .get();

        return KubernetesSupport.verifyPodStatus(pod, phase) ? pod : null;
    }

    /**
     * Retrieve pod given state selected by label key and value expression.
     * @param labelExpression
     * @param phase
     * @return
     */
    private Pod getPodFromLabel(String labelExpression, String phase) {
        if (labelExpression == null || labelExpression.isEmpty()) {
            return null;
        }

        String[] tokens = labelExpression.split("=");
        String labelKey = tokens[0];
        String labelValue = tokens.length > 1 ? tokens[1] : "";

        PodList pods = getKubernetesClient().pods()
                .inNamespace(KubernetesSettings.getNamespace())
                .withLabel(labelKey, labelValue)
                .list();

        return pods.getItems().stream()
                .filter(pod -> KubernetesSupport.verifyPodStatus(pod, phase))
                .findFirst()
                .orElse(null);
    }

    /**
     * If name is set return as pod name. Else return given label expression.
     * @param name
     * @param labelExpression
     * @return
     */
    private String getNameOrLabel(String name, String labelExpression) {
        if (name != null && !name.isEmpty()) {
            return name;
        } else {
            return labelExpression;
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKubernetesAction.Builder<VerifyPodAction, Builder> {

        private String podName;
        private String labelExpression;
        private String logMessage;

        private int maxAttempts = KubernetesSettings.getMaxAttempts();
        private long delayBetweenAttempts = KubernetesSettings.getDelayBetweenAttempts();

        private String phase = "Running";

        public Builder isRunning() {
            this.phase = "Running";
            return this;
        }

        public Builder isStopped() {
            this.phase = "Stopped";
            return this;
        }

        public Builder podName(String podName) {
            this.podName = podName;
            return this;
        }

        public Builder label(String name, String value) {
            this.labelExpression = String.format("%s=%s", name, value);
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
        public VerifyPodAction build() {
            return new VerifyPodAction(this);
        }
    }
}
