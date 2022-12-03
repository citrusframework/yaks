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

import java.util.Map;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action verifies integration Pod running/stopped state and optionally waits for a log message to be present. Raises errors
 * when either the integration is not in expected state or the log message is not available. Both operations are automatically retried
 * for a given amount of attempts.
 *
 * @author Christoph Deppisch
 */
public class VerifyIntegrationAction extends AbstractCamelKAction {

    private static final Logger INTEGRATION_STATUS_LOG = LoggerFactory.getLogger("INTEGRATION_STATUS");
    private static final Logger INTEGRATION_LOG = LoggerFactory.getLogger("INTEGRATION_LOGS");

    private final String integrationName;
    private final String logMessage;
    private final int maxAttempts;
    private final long delayBetweenAttempts;

    private final String phase;
    private final boolean printLogs;

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
        this.printLogs = builder.printLogs;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(integrationName);

        LOG.info(String.format("Verify Camel K integration '%s'", name));

        if (YaksSettings.isLocal(clusterType(context))) {
            verifyLocalIntegration(name, context);
        } else {
            verifyIntegration(namespace(context), name, context);
        }

        LOG.info(String.format("Successfully verified Camel K integration '%s'", name));
    }

    private void verifyLocalIntegration(String integration, TestContext context) {
        Long pid = verifyLocalIntegrationStatus(integration, context.replaceDynamicContentInString(phase), context);

        if (logMessage != null) {
            verifyLocalIntegrationLogs(pid, integration, context.replaceDynamicContentInString(logMessage), context);
        }
    }

    private void verifyLocalIntegrationLogs(Long pid, String integration, String message, TestContext context) {
        if (printLogs) {
            INTEGRATION_LOG.info(String.format("Waiting for integration '%s' to log message", integration));
        }

        String log;
        int offset = 0;

        ProcessAndOutput pao = context.getVariable(integration + ":process:" + pid, ProcessAndOutput.class);
        for (int i = 0; i < maxAttempts; i++) {
            log = pao.getOutput();

            if (printLogs && (offset < log.length())) {
                INTEGRATION_LOG.info(log.substring(offset));
                offset = log.length();
            }

            if (log.contains(message)) {
                LOG.info("Verified integration logs - All values OK!");
                return;
            }

            if (!printLogs) {
                LOG.warn(String.format("Waiting for integration '%s' to log message - retry in %s ms", integration, delayBetweenAttempts));
            }

            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for integration logs", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify integration '%s' - " +
                        "has not printed message '%s' after %d attempts", integration, message, maxAttempts)));
    }

    private Long verifyLocalIntegrationStatus(String integration, String phase, TestContext context) {
        INTEGRATION_STATUS_LOG.info(String.format("Waiting for integration '%s' to be in state '%s'", integration, phase));

        for (int i = 0; i < maxAttempts; i++) {
            if (context.getVariables().containsKey(integration + ":pid")) {
                Long pid = context.getVariable(integration + ":pid", Long.class);
                Map<String, String> properties = camel().get(pid);
                if ((phase.equals("Stopped") && properties.isEmpty()) || (!properties.isEmpty() && properties.get("STATUS").equals(phase))) {
                    LOG.info(String.format("Verified integration '%s' state '%s' - All values OK!", integration, phase));
                    return pid;
                }
            }

            LOG.info(String.format("Waiting for integration '%s' to be in state '%s'- retry in %s ms", integration, phase, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for integration state", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify integration '%s' - " +
                        "is not in state '%s' after %d attempts", integration, phase, maxAttempts)));

    }

    private void verifyIntegration(String namespace, String integration, TestContext context) {
        Pod pod = verifyIntegrationPod(integration, context.replaceDynamicContentInString(phase), namespace);

        if (logMessage != null) {
            verifyIntegrationLogs(pod, integration, namespace, context.replaceDynamicContentInString(logMessage));
        }
    }

    /**
     * Wait for integration pod to log given message.
     * @param pod
     * @param name
     * @param namespace
     * @param message
     */
    private void verifyIntegrationLogs(Pod pod, String name, String namespace, String message) {
        if (printLogs) {
            INTEGRATION_LOG.info(String.format("Waiting for pod '%s' to log message", name));
        }

        String log;
        int offset = 0;

        for (int i = 0; i < maxAttempts; i++) {
            log = getIntegrationPodLogs(pod, namespace);

            if (printLogs && (offset < log.length())) {
                INTEGRATION_LOG.info(log.substring(offset));
                offset = log.length();
            }

            if (log.contains(message)) {
                LOG.info("Verified integration logs - All values OK!");
                return;
            }

            if (!printLogs) {
                LOG.warn(String.format("Waiting for integration '%s' to log message - retry in %s ms", name, delayBetweenAttempts));
            }

            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for integration pod logs", e);
            }
        }

        throw new ActionTimeoutException((maxAttempts * delayBetweenAttempts),
                new CitrusRuntimeException(String.format("Failed to verify integration '%s' - " +
                        "has not printed message '%s' after %d attempts", name, message, maxAttempts)));
    }

    /**
     * Retrieve log messages from given pod.
     * @param pod
     * @param namespace
     * @return
     */
    private String getIntegrationPodLogs(Pod pod, String namespace) {
        PodResource<Pod> podRes = getKubernetesClient().pods()
                .inNamespace(namespace)
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
     * @param namespace
     * @return
     */
    private Pod verifyIntegrationPod(String name, String phase, String namespace) {
        INTEGRATION_STATUS_LOG.info(String.format("Waiting for integration '%s' to be in state '%s'", name, phase));

        for (int i = 0; i < maxAttempts; i++) {
            Pod pod = getIntegrationPod(name, phase, namespace);
            if (pod != null) {
                LOG.info(String.format("Verified integration pod '%s' state '%s' - All values OK!", name, phase));
                return pod;
            }

            LOG.info(String.format("Waiting for integration '%s' to be in state '%s'- retry in %s ms", name, phase, delayBetweenAttempts));
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
     * @param namespace
     * @return
     */
    private Pod getIntegrationPod(final String integration, final String phase, final String namespace) {
        PodList pods = getKubernetesClient().pods()
                .inNamespace(namespace)
                .withLabel(CamelKSettings.INTEGRATION_LABEL, integration)
                .list();

        if (pods.getItems().isEmpty()) {
            INTEGRATION_STATUS_LOG.info(String.format("Integration '%s' not yet available. Will keep checking ...", integration));
        }

        return pods.getItems().stream()
                .filter(pod -> {
                    boolean verified = KubernetesSupport.verifyPodStatus(pod, phase);

                    if (!verified) {
                        INTEGRATION_STATUS_LOG.info(String.format("Integration '%s' not yet in state '%s'. Will keep checking ...", integration, phase));
                    }

                    return verified;
                })
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
        private boolean printLogs = true;

        public Builder isRunning() {
            this.phase = "Running";
            return this;
        }

        public Builder isStopped() {
            this.phase = "Stopped";
            return this;
        }

        public Builder printLogs(boolean printLogs) {
            this.printLogs = printLogs;
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
