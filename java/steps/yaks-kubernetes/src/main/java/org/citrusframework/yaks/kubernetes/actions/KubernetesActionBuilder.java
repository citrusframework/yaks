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
     * Performs actions on Kubernetes services.
     * @return
     */
    public KubernetesActionBuilder.ServiceActionBuilder services() {
        return new ServiceActionBuilder();
    }

    /**
     * Performs actions on Kubernetes resources.
     * @return
     */
    public KubernetesActionBuilder.ResourceActionBuilder resources() {
        return new ResourceActionBuilder();
    }

    /**
     * Performs actions on Kubernetes pods.
     * @return
     */
    public KubernetesActionBuilder.PodActionBuilder pods() {
        return new PodActionBuilder();
    }

    /**
     * Performs actions on Kubernetes custom resources.
     * @return
     */
    public KubernetesActionBuilder.CustomResourceActionBuilder customResources() {
        return new CustomResourceActionBuilder();
    }

    /**
     * Performs actions on Kubernetes secrets.
     * @return
     */
    public KubernetesActionBuilder.SecretActionBuilder secrets() {
        return new SecretActionBuilder();
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

    public class SecretActionBuilder {
        /**
         * Create secret instance.
         * @param secretName the name of the Kubernetes secret.
         */
        public CreateSecretAction.Builder create(String secretName) {
            CreateSecretAction.Builder builder = new CreateSecretAction.Builder()
                    .client(kubernetesClient)
                    .name(secretName);
            delegate = builder;
            return builder;
        }

        /**
         * Delete secret instance.
         * @param secretName the name of the Kubernetes secret.
         */
        public DeleteSecretAction.Builder delete(String secretName) {
            DeleteSecretAction.Builder builder = new DeleteSecretAction.Builder()
                    .client(kubernetesClient)
                    .name(secretName);
            delegate = builder;
            return builder;
        }
    }

    public class CustomResourceActionBuilder {
        /**
         * Create custom resource instance.
         */
        public CreateCustomResourceAction.Builder create() {
            CreateCustomResourceAction.Builder builder = new CreateCustomResourceAction.Builder()
                    .client(kubernetesClient);
            delegate = builder;
            return builder;
        }

        /**
         * Delete custom resource instance.
         * @param name the name of the Kubernetes custom resource.
         */
        public DeleteCustomResourceAction.Builder delete(String name) {
            DeleteCustomResourceAction.Builder builder = new DeleteCustomResourceAction.Builder()
                    .client(kubernetesClient)
                    .resourceName(name);
            delegate = builder;
            return builder;
        }

        /**
         * Verify that given custom resource matches a condition.
         * @param name the name of the custom resource.
         */
        public VerifyCustomResourceAction.Builder verify(String name) {
            VerifyCustomResourceAction.Builder builder = new VerifyCustomResourceAction.Builder()
                    .client(kubernetesClient)
                    .resourceName(name);
            delegate = builder;
            return builder;
        }

        /**
         * Verify that given custom resource matches a condition.
         * @param label the label to filter results.
         * @param value the value of the label.
         */
        public VerifyCustomResourceAction.Builder verify(String label, String value) {
            VerifyCustomResourceAction.Builder builder = new VerifyCustomResourceAction.Builder()
                    .client(kubernetesClient)
                    .label(label, value);
            delegate = builder;
            return builder;
        }
    }

    public class PodActionBuilder {
        /**
         * Verify that given pod is running.
         * @param podName the name of the Camel-K pod.
         */
        public VerifyPodAction.Builder verify(String podName) {
            VerifyPodAction.Builder builder = new VerifyPodAction.Builder()
                    .client(kubernetesClient)
                    .podName(podName);
            delegate = builder;
            return builder;
        }

        /**
         * Verify that given pod is running.
         * @param label the name of the pod label to filter on.
         * @param value the value of the pod label to match.
         */
        public VerifyPodAction.Builder verify(String label, String value) {
            VerifyPodAction.Builder builder = new VerifyPodAction.Builder()
                    .client(kubernetesClient)
                    .label(label, value);
            delegate = builder;
            return builder;
        }
    }

    public class ResourceActionBuilder {
        /**
         * Create any Kubernetes resource instance from yaml.
         */
        public CreateResourceAction.Builder create() {
            CreateResourceAction.Builder builder = new CreateResourceAction.Builder()
                    .client(kubernetesClient);
            delegate = builder;
            return builder;
        }

        /**
         * Delete any Kubernetes resource instance.
         * @param content the Kubernetes resource as YAML content.
         */
        public DeleteResourceAction.Builder delete(String content) {
            DeleteResourceAction.Builder builder = new DeleteResourceAction.Builder()
                    .client(kubernetesClient)
                    .content(content);
            delegate = builder;
            return builder;
        }
    }

    public class ServiceActionBuilder {

        /**
         * Create service instance.
         * @param serviceName the name of the Kubernetes service.
         */
        public CreateServiceAction.Builder create(String serviceName) {
            CreateServiceAction.Builder builder = new CreateServiceAction.Builder()
                    .client(kubernetesClient)
                    .name(serviceName);
            delegate = builder;
            return builder;
        }

        /**
         * Delete service instance.
         * @param serviceName the name of the Kubernetes service.
         */
        public DeleteServiceAction.Builder delete(String serviceName) {
            DeleteServiceAction.Builder builder = new DeleteServiceAction.Builder()
                    .client(kubernetesClient)
                    .name(serviceName);
            delegate = builder;
            return builder;
        }


    }
}
