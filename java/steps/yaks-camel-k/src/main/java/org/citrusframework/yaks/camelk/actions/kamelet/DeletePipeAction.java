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

import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBinding;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBindingList;
import org.citrusframework.yaks.camelk.model.Pipe;
import org.citrusframework.yaks.camelk.model.PipeList;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * @author Christoph Deppisch
 */
public class DeletePipeAction extends AbstractKameletAction {

    private final String pipeName;

    public DeletePipeAction(Builder builder) {
        super("delete-pipe", builder);

        this.pipeName = builder.pipeName;
    }

    @Override
    public void doExecute(TestContext context) {
        String pipe = context.replaceDynamicContentInString(pipeName);

        LOG.info(String.format("Deleting pipe '%s'", pipe));

        if (YaksSettings.isLocal(clusterType(context))) {
            deleteLocalBinding(pipe, context);
        } else {
            deleteBinding(getKubernetesClient(), namespace(context), pipe, context);
        }

        LOG.info(String.format("Successfully deleted pipe '%s'", pipe));
    }

    private void deleteBinding(KubernetesClient k8sClient, String namespace, String name, TestContext context) {
        if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
            k8sClient.resources(KameletBinding.class, KameletBindingList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .delete();
        } else {
            k8sClient.resources(Pipe.class, PipeList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .delete();
        }
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
                    .orElseThrow(() -> new CitrusRuntimeException(String.format("Unable to retrieve pipe process id %s:pid", name)));
        }

        camel().stop(pid);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKameletAction.Builder<DeletePipeAction, Builder> {

        private String pipeName;

        public Builder binding(String name) {
            apiVersion(CamelKSettings.V1ALPHA1);
            this.pipeName = name;
            return this;
        }

        public Builder pipe(String name) {
            this.pipeName = name;
            return this;
        }

        @Override
        public DeletePipeAction build() {
            return new DeletePipeAction(this);
        }
    }
}
