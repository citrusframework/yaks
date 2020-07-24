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

package org.citrusframework.yaks.camelk.actions;

import com.consol.citrus.AbstractTestActionBuilder;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ActionTimeoutException;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test action verifies integration Pod running state and optionally waits for a log message to be present. Raises errors
 * when either the integration is not in running state or the log message is not available. Both operations are automatically retried
 * for a given amount of attempts.
 *
 * @author Christoph Deppisch
 */
public class VerifyIntegrationTestAction extends AbstractTestAction {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(VerifyIntegrationTestAction.class);

    private final KubernetesClient client;
    private final String integrationName;
    private final String logMessage;
    private final int maxAttempts;
    private final long delayBetweenAttempts;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyIntegrationTestAction(Builder builder) {
        super("verify-integration", builder);
        this.client = builder.client;
        this.integrationName = builder.integrationName;
        this.logMessage = builder.logMessage;
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
    }

    @Override
    public void doExecute(TestContext context) {
        String podName = context.replaceDynamicContentInString(integrationName);
        Pod pod = verifyRunningIntegrationPod(podName);

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

        throw new ActionTimeoutException(String.format("Failed to verify integration '%s' - has not printed message '%s' after %d attempts", name, logMessage, maxAttempts));
    }

    /**
     * Retrieve log messages from given pod.
     * @param pod
     * @return
     */
    private String getIntegrationPodLogs(Pod pod) {
        PodResource<Pod, DoneablePod> podRes = client.pods()
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
     * Wait for given pod to be in running state.
     * @param name
     * @return
     */
    private Pod verifyRunningIntegrationPod(String name) {
        for (int i = 0; i < maxAttempts; i++) {
            Pod pod = getRunningIntegrationPod(name);
            if (pod != null) {
                LOG.info(String.format("Verified integration pod '%s' state running!", name));
                return pod;
            }

            LOG.warn(String.format("Waiting for running integration '%s' - retry in %s ms", name, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for integration pod state", e);
            }
        }

        throw new ActionTimeoutException(String.format("Failed to verify integration '%s' - is not running after %d attempts", name, maxAttempts));
    }

    /**
     * Retrieve pod running state.
     * @param integration
     * @return
     */
    private Pod getRunningIntegrationPod(String integration) {
        PodList pods = client.pods().inNamespace(CamelKSettings.getNamespace()).withLabel("camel.apache.org/integration", integration).list();
        if (pods.getItems().size() == 0) {
            return null;
        }
        for (Pod p : pods.getItems()) {
            if (p.getStatus() != null
                    && "Running".equals(p.getStatus().getPhase())
                    && p.getStatus().getContainerStatuses().stream().allMatch(status -> status.getReady())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractTestActionBuilder<VerifyIntegrationTestAction, Builder> {

        private KubernetesClient client = new DefaultKubernetesClient();
        private String integrationName;
        private String logMessage;

        private int maxAttempts = CamelKSettings.getMaxAttempts();
        private long delayBetweenAttempts = CamelKSettings.getDelayBetweenAttempts();

        /**
         * Fluent API action building entry method used in Java DSL.
         * @return
         */
        public static Builder verifyIntegration() {
            return new Builder();
        }

        public Builder client(KubernetesClient client) {
            this.client = client;
            return this;
        }

        public Builder isRunning(String integrationName) {
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
        public VerifyIntegrationTestAction build() {
            return new VerifyIntegrationTestAction(this);
        }
    }
}
