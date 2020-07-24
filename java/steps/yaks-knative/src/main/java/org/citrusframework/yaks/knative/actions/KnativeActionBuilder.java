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

package org.citrusframework.yaks.knative.actions;

import com.consol.citrus.TestActionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.knative.actions.eventing.CreateBrokerAction;
import org.citrusframework.yaks.knative.actions.eventing.CreateTriggerAction;
import org.citrusframework.yaks.knative.actions.eventing.VerifyBrokerAction;
import org.citrusframework.yaks.knative.actions.messaging.CreateChannelAction;
import org.citrusframework.yaks.knative.actions.messaging.CreateSubscriptionAction;
import org.citrusframework.yaks.knative.actions.serving.CreateServiceAction;
import org.citrusframework.yaks.knative.actions.serving.DeleteServiceAction;
import org.springframework.util.Assert;

/**
 * @author Christoph Deppisch
 */
public class KnativeActionBuilder implements TestActionBuilder.DelegatingTestActionBuilder<KnativeAction> {

    /** Kubernetes client */
    private KubernetesClient kubernetesClient;

    private AbstractKnativeAction.Builder<? extends KnativeAction, ?> delegate;

    /**
     * Fluent API action building entry method used in Java DSL.
     * @return
     */
    public static KnativeActionBuilder knative() {
        return new KnativeActionBuilder();
    }

    /**
     * Use a custom Kubernetes client.
     */
    public KnativeActionBuilder client(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        return this;
    }

    /**
     * Create service instance.
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
     */
    public DeleteServiceAction.Builder deleteService(String serviceName) {
        DeleteServiceAction.Builder builder = new DeleteServiceAction.Builder()
                .client(kubernetesClient)
                .name(serviceName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create channel instance.
     */
    public CreateChannelAction.Builder createChannel(String channelName) {
        CreateChannelAction.Builder builder = new CreateChannelAction.Builder()
                .client(kubernetesClient)
                .name(channelName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete channel instance.
     */
    public DeleteKnativeResourceAction.Builder deleteChannel(String channelName) {
        DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                .client(kubernetesClient)
                .component("messaging")
                .kind("channels")
                .name(channelName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create subscription instance.
     */
    public CreateSubscriptionAction.Builder createSubscription(String subscriptionName) {
        CreateSubscriptionAction.Builder builder = new CreateSubscriptionAction.Builder()
                .client(kubernetesClient)
                .name(subscriptionName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete subscription instance.
     */
    public DeleteKnativeResourceAction.Builder deleteSubscription(String subscriptionName) {
        DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                .client(kubernetesClient)
                .component("messaging")
                .kind("subscriptions")
                .name(subscriptionName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create trigger instance.
     */
    public CreateTriggerAction.Builder createTrigger(String triggerName) {
        CreateTriggerAction.Builder builder = new CreateTriggerAction.Builder()
                .client(kubernetesClient)
                .name(triggerName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete trigger instance.
     */
    public DeleteKnativeResourceAction.Builder deleteTrigger(String triggerName) {
        DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                .client(kubernetesClient)
                .component("eventing")
                .kind("triggers")
                .name(triggerName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Create broker instance.
     */
    public CreateBrokerAction.Builder createBroker(String brokerName) {
        CreateBrokerAction.Builder builder = new CreateBrokerAction.Builder()
                .client(kubernetesClient)
                .name(brokerName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete broker instance.
     */
    public DeleteKnativeResourceAction.Builder deleteBroker(String brokerName) {
        DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                .client(kubernetesClient)
                .component("eventing")
                .kind("brokers")
                .name(brokerName);
        this.delegate = builder;
        return builder;
    }

    /**
     * Delete broker instance.
     */
    public VerifyBrokerAction.Builder verifyBroker(String brokerName) {
        VerifyBrokerAction.Builder builder = new VerifyBrokerAction.Builder()
                .client(kubernetesClient)
                .name(brokerName);
        this.delegate = builder;
        return builder;
    }

    @Override
    public KnativeAction build() {
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
