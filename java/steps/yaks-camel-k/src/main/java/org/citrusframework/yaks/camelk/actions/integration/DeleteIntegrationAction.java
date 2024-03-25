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

package org.citrusframework.yaks.camelk.actions.integration;

import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.IntegrationList;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * @author Christoph Deppisch
 */
public class DeleteIntegrationAction extends AbstractCamelKAction {

    private final String integrationName;

    public DeleteIntegrationAction(Builder builder) {
        super("delete-integration", builder);

        this.integrationName = builder.integrationName;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(integrationName);

        LOG.info(String.format("Deleting Camel K integration '%s'", name));

        if (YaksSettings.isLocal(clusterType(context))) {
            deleteLocalIntegration(name, context);
        } else {
            deleteIntegration(getKubernetesClient(), namespace(context), name);
        }

        LOG.info(String.format("Successfully deleted Camel K integration '%s'", name));
    }

    /**
     * Deletes the Camel K integration custom resource in given namespace.
     * @param k8sClient
     * @param namespace
     * @param name
     */
    private static void deleteIntegration(KubernetesClient k8sClient, String namespace, String name) {
        k8sClient.resources(Integration.class, IntegrationList.class)
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    /**
     * Deletes the Camel K integration from local JBang runtime.
     * @param name
     * @param context
     */
    private static void deleteLocalIntegration(String name, TestContext context) {
        Long pid;
        if (context.getVariables().containsKey(name + ":pid")) {
            pid = context.getVariable(name + ":pid", Long.class);
        } else {
            pid = camel().getAll().stream()
                    .filter(props -> name.equals(props.get("NAME")) && !props.getOrDefault("PID", "").isBlank())
                    .map(props -> Long.valueOf(props.get("PID"))).findFirst()
                    .orElseThrow(() -> new CitrusRuntimeException(String.format("Unable to retrieve integration process id %s:pid", name)));
        }

        camel().stop(pid);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractCamelKAction.Builder<DeleteIntegrationAction, Builder> {

        private String integrationName;

        public Builder integration(String integrationName) {
            this.integrationName = integrationName;
            return this;
        }

        @Override
        public DeleteIntegrationAction build() {
            return new DeleteIntegrationAction(this);
        }
    }
}
