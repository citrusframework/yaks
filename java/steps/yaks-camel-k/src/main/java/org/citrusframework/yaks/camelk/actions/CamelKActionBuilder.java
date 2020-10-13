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

import com.consol.citrus.TestActionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.camelk.actions.integration.CreateIntegrationAction;
import org.citrusframework.yaks.camelk.actions.integration.VerifyIntegrationAction;
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
     * @param integrationName the name of the Camel-K integration.
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
     * @param integrationName the name of the Camel-K integration.
     */
    public DeleteCamelKResourceAction.Builder deleteIntegration(String integrationName) {
        DeleteCamelKResourceAction.Builder builder = new DeleteCamelKResourceAction.Builder()
                .client(kubernetesClient)
                .kind("integrations")
                .resource(integrationName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Verify that given integration is running.
     * @param integrationName the name of the Camel-K integration.
     */
    public VerifyIntegrationAction.Builder verifyIntegration(String integrationName) {
        VerifyIntegrationAction.Builder builder = new VerifyIntegrationAction.Builder()
                .client(kubernetesClient)
                .isRunning(integrationName);
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
