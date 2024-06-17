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

package org.citrusframework.yaks.camelk.actions;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.TestActionBuilder;
import org.citrusframework.yaks.camelk.actions.integration.CreateIntegrationAction;
import org.citrusframework.yaks.camelk.actions.integration.DeleteIntegrationAction;
import org.citrusframework.yaks.camelk.actions.integration.VerifyIntegrationAction;
import org.citrusframework.yaks.camelk.actions.kamelet.CreateKameletAction;
import org.citrusframework.yaks.camelk.actions.kamelet.CreateKameletBindingAction;
import org.citrusframework.yaks.camelk.actions.kamelet.CreatePipeAction;
import org.citrusframework.yaks.camelk.actions.kamelet.DeleteKameletAction;
import org.citrusframework.yaks.camelk.actions.kamelet.DeleteKameletBindingAction;
import org.citrusframework.yaks.camelk.actions.kamelet.DeletePipeAction;
import org.citrusframework.yaks.camelk.actions.kamelet.VerifyKameletAction;
import org.citrusframework.yaks.camelk.actions.kamelet.VerifyKameletBindingAction;
import org.citrusframework.yaks.camelk.actions.kamelet.VerifyPipeAction;
import org.springframework.util.Assert;

/**
 * @author Christoph Deppisch
 */
public class CamelKActionBuilder implements TestActionBuilder.DelegatingTestActionBuilder<CamelKAction> {

    /** Kubernetes client */
    private KubernetesClient kubernetesClient;

    private AbstractCamelKAction.Builder<? extends CamelKAction, ?> delegate;

    /**
     * Fluent API action building entry method used in Java DSL.
     * @return
     */
    public static CamelKActionBuilder camelk() {
        return new CamelKActionBuilder();
    }

    /**
     * Use a custom Kubernetes client.
     */
    public CamelKActionBuilder client(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        return this;
    }

    /**
     * Create integration instance.
     * @param integrationName the name of the Camel K integration.
     */
    public CreateIntegrationAction.Builder createIntegration(String integrationName) {
        CreateIntegrationAction.Builder builder = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration(integrationName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete integration instance.
     * @param integrationName the name of the Camel K integration.
     */
    public DeleteIntegrationAction.Builder deleteIntegration(String integrationName) {
        DeleteIntegrationAction.Builder builder = new DeleteIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration(integrationName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create kamelet CRD in current namespace.
     * @param kameletName the name of the Kamelet.
     */
    public CreateKameletAction.Builder createKamelet(String kameletName) {
        CreateKameletAction.Builder builder = new CreateKameletAction.Builder()
                .client(kubernetesClient)
                .kamelet(kameletName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete kamelet CRD from current namespace.
     * @param kameletName the name of the Kamelet.
     */
    public DeleteKameletAction.Builder deleteKamelet(String kameletName) {
        DeleteKameletAction.Builder builder = new DeleteKameletAction.Builder()
                .client(kubernetesClient)
                .kamelet(kameletName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create pipe CRD in current namespace.
     * @param pipeName the name of the pipe.
     */
    public CreatePipeAction.Builder createPipe(String pipeName) {
        CreatePipeAction.Builder builder = new CreatePipeAction.Builder()
                .client(kubernetesClient)
                .pipe(pipeName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete pipe CRD from current namespace.
     * @param pipeName the name of the pipe.
     */
    public DeletePipeAction.Builder deletePipe(String pipeName) {
        DeletePipeAction.Builder builder = new DeletePipeAction.Builder()
                .client(kubernetesClient)
                .pipe(pipeName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create binding CRD in current namespace.
     * @param bindingName the name of the binding.
     */
    public CreateKameletBindingAction.Builder createKameletBinding(String bindingName) {
        CreateKameletBindingAction.Builder builder = new CreateKameletBindingAction.Builder()
                .client(kubernetesClient)
                .binding(bindingName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete binding CRD from current namespace.
     * @param bindingName the name of the binding.
     */
    public DeleteKameletBindingAction.Builder deleteKameletBinding(String bindingName) {
        DeleteKameletBindingAction.Builder builder = new DeleteKameletBindingAction.Builder()
                .client(kubernetesClient)
                .binding(bindingName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Verify that given integration is running.
     * @param integrationName the name of the Camel K integration.
     */
    public VerifyIntegrationAction.Builder verifyIntegration(String integrationName) {
        VerifyIntegrationAction.Builder builder = new VerifyIntegrationAction.Builder()
                .client(kubernetesClient)
                .integrationName(integrationName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Verify that given Kamelet CRD is available in current namespace.
     * @param kameletName the name of the Kamelet.
     */
    public VerifyKameletAction.Builder verifyKamelet(String kameletName) {
        VerifyKameletAction.Builder builder = new VerifyKameletAction.Builder()
                .client(kubernetesClient)
                .kameletName(kameletName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Verify that given pipe CRD is available in current namespace.
     * @param pipeName the name of the pipe.
     */
    public VerifyPipeAction.Builder verifyPipe(String pipeName) {
        VerifyPipeAction.Builder builder = new VerifyPipeAction.Builder()
                .client(kubernetesClient)
                .isAvailable(pipeName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Verify that given binding CRD is available in current namespace.
     * @param bindingName the name of the binding.
     */
    public VerifyKameletBindingAction.Builder verifyKameletBinding(String bindingName) {
        VerifyKameletBindingAction.Builder builder = new VerifyKameletBindingAction.Builder()
                .client(kubernetesClient)
                .isAvailable(bindingName);
        this.delegate = builder;
        return builder;
    }

    @Override
    public CamelKAction build() {
        Assert.notNull(delegate, "Missing delegate action to build");
        if (kubernetesClient != null) {
            delegate.client(kubernetesClient);
        }
        return delegate.build();
    }

    @Override
    public TestActionBuilder<?> getDelegate() {
        return delegate;
    }
}
