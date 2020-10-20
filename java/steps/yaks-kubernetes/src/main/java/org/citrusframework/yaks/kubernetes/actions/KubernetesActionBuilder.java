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

import com.consol.citrus.TestActionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.util.Assert;

/**
 * @author Christoph Deppisch
 */
public class KubernetesActionBuilder implements TestActionBuilder.DelegatingTestActionBuilder<KubernetesAction> {

    /** Kubernetes client */
    private KubernetesClient kubernetesClient;

    private AbstractKubernetesAction.Builder<? extends KubernetesAction, ?> delegate;

    /**
     * Fluent API action building entry method used in Java DSL.
     * @return
     */
    public static KubernetesActionBuilder k8s() {
        return kubernetes();
    }

    /**
     * Fluent API action building entry method used in Java DSL.
     * @return
     */
    public static KubernetesActionBuilder kubernetes() {
        return new KubernetesActionBuilder();
    }

    /**
     * Use a custom Kubernetes client.
     * @param kubernetesClient
     */
    public KubernetesActionBuilder client(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        return this;
    }

    /**
     * Create service instance.
     * @param serviceName the name of the Kubernetes service.
     */
    public CreateServiceAction.Builder createService(String serviceName) {
        CreateServiceAction.Builder builder = new CreateServiceAction.Builder()
                .client(kubernetesClient)
                .name(serviceName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete service instance.
     * @param serviceName the name of the Kubernetes service.
     */
    public DeleteServiceAction.Builder deleteService(String serviceName) {
        DeleteServiceAction.Builder builder = new DeleteServiceAction.Builder()
                .client(kubernetesClient)
                .name(serviceName);
        this.delegate = builder;
        return builder;
    }

    @Override
    public KubernetesAction build() {
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
