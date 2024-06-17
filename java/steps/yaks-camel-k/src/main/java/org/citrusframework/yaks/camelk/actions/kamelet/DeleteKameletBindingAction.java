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

package org.citrusframework.yaks.camelk.actions.kamelet;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.camel.v1alpha1.KameletBinding;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBindingList;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * @author Christoph Deppisch
 */
public class DeleteKameletBindingAction extends AbstractKameletAction {

    private final String bindingName;

    public DeleteKameletBindingAction(Builder builder) {
        super("delete-binding", builder);

        this.bindingName = builder.bindingName;
    }

    @Override
    public void doExecute(TestContext context) {
        String binding = context.replaceDynamicContentInString(bindingName);

        LOG.info(String.format("Deleting binding '%s'", binding));

        if (YaksSettings.isLocal(clusterType(context))) {
            deleteLocalBinding(binding, context);
        } else {
            deleteBinding(getKubernetesClient(), namespace(context), binding, context);
        }

        LOG.info(String.format("Successfully deleted binding '%s'", binding));
    }

    private void deleteBinding(KubernetesClient k8sClient, String namespace, String name, TestContext context) {
        k8sClient.resources(KameletBinding.class, KameletBindingList.class)
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    /**
     * Deletes the Camel K integration from local JBang runtime.
     * @param name
     * @param context
     */
    private static void deleteLocalBinding(String name, TestContext context) {
        Long pid;
        if (context.getVariables().containsKey(name + ":pid")) {
            pid = context.getVariable(name + ":pid", Long.class);
        } else {
            pid = camel().getAll().stream()
                    .filter(props -> name.equals(props.get("NAME")) && !props.getOrDefault("PID", "").isBlank())
                    .map(props -> Long.valueOf(props.get("PID"))).findFirst()
                    .orElseThrow(() -> new CitrusRuntimeException(String.format("Unable to retrieve binding process id %s:pid", name)));
        }

        camel().stop(pid);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKameletAction.Builder<DeleteKameletBindingAction, Builder> {

        private String bindingName;

        public Builder binding(String name) {
            apiVersion(CamelKSettings.V1ALPHA1);
            this.bindingName = name;
            return this;
        }

        @Override
        public DeleteKameletBindingAction build() {
            return new DeleteKameletBindingAction(this);
        }
    }
}
