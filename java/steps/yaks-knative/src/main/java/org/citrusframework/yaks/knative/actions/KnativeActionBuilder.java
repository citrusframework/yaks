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

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.TestActionBuilder;
import org.citrusframework.yaks.knative.actions.eventing.CreateBrokerAction;
import org.citrusframework.yaks.knative.actions.eventing.CreateTriggerAction;
import org.citrusframework.yaks.knative.actions.eventing.DeleteBrokerAction;
import org.citrusframework.yaks.knative.actions.eventing.VerifyBrokerAction;
import org.citrusframework.yaks.knative.actions.messaging.CreateChannelAction;
import org.citrusframework.yaks.knative.actions.messaging.CreateSubscriptionAction;
import org.springframework.util.Assert;

/**
 * @author Christoph Deppisch
 */
public class KnativeActionBuilder implements TestActionBuilder.DelegatingTestActionBuilder<KnativeAction> {

    /** Kubernetes client */
    private KubernetesClient kubernetesClient;
    private KnativeClient knativeClient;

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
     * @param kubernetesClient
     */
    public KnativeActionBuilder client(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        return this;
    }

    /**
     * Use a custom Knative client.
     * @param knativeClient
     */
    public KnativeActionBuilder client(KnativeClient knativeClient) {
        this.knativeClient = knativeClient;
        return this;
    }

    /**
     * Performs action on Knative channels.
     * @return
     */
    public ChannelActionBuilder channels() {
        return new ChannelActionBuilder();
    }

    /**
     * Performs action on Knative subscriptions.
     * @return
     */
    public SubscriptionActionBuilder subscriptions() {
        return new SubscriptionActionBuilder();
    }

    /**
     * Performs action on Knative trigger.
     * @return
     */
    public TriggerActionBuilder trigger() {
        return new TriggerActionBuilder();
    }

    /**
     * Performs action on Knative brokers.
     * @return
     */
    public BrokerActionBuilder brokers() {
        return new BrokerActionBuilder();
    }

    @Override
    public KnativeAction build() {
        Assert.notNull(delegate, "Missing delegate action to build");
        if (kubernetesClient != null) {
            delegate.client(kubernetesClient);
        }

        if (knativeClient != null) {
            delegate.client(knativeClient);
        }
        return delegate.build();
    }

    @Override
    public TestActionBuilder<?> getDelegate() {
        return delegate;
    }

    public class ChannelActionBuilder {
        /**
         * Create channel instance.
         * @param channelName the name of the Knative channel.
         */
        public CreateChannelAction.Builder create(String channelName) {
            CreateChannelAction.Builder builder = new CreateChannelAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .name(channelName);
            delegate = builder;
            return builder;
        }

        /**
         * Delete channel instance.
         * @param channelName the name of the Knative channel.
         */
        public DeleteKnativeResourceAction.Builder delete(String channelName) {
            DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .component("messaging")
                    .kind("channels")
                    .name(channelName);
            delegate = builder;
            return builder;
        }
    }

    public class SubscriptionActionBuilder {
        /**
         * Create subscription instance.
         * @param subscriptionName the name of the Knative subscription.
         */
        public CreateSubscriptionAction.Builder create(String subscriptionName) {
            CreateSubscriptionAction.Builder builder = new CreateSubscriptionAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .name(subscriptionName);
            delegate = builder;
            return builder;
        }

        /**
         * Delete subscription instance.
         * @param subscriptionName the name of the Knative subscription.
         */
        public DeleteKnativeResourceAction.Builder delete(String subscriptionName) {
            DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .component("messaging")
                    .kind("subscriptions")
                    .name(subscriptionName);
            delegate = builder;
            return builder;
        }
    }

    public class TriggerActionBuilder {
        /**
         * Create trigger instance.
         * @param triggerName the name of the Knative trigger.
         */
        public CreateTriggerAction.Builder create(String triggerName) {
            CreateTriggerAction.Builder builder = new CreateTriggerAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .name(triggerName);
            delegate = builder;
            return builder;
        }

        /**
         * Delete trigger instance.
         * @param triggerName the name of the Knative trigger.
         */
        public DeleteKnativeResourceAction.Builder delete(String triggerName) {
            DeleteKnativeResourceAction.Builder builder = new DeleteKnativeResourceAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .component("eventing")
                    .kind("triggers")
                    .name(triggerName);
            delegate = builder;
            return builder;
        }
    }

    public class BrokerActionBuilder {
        /**
         * Create broker instance.
         * @param brokerName the name of the Knative broker.
         */
        public CreateBrokerAction.Builder create(String brokerName) {
            CreateBrokerAction.Builder builder = new CreateBrokerAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .name(brokerName);
            delegate = builder;
            return builder;
        }

        /**
         * Delete broker instance.
         * @param brokerName the name of the Knative broker.
         */
        public DeleteBrokerAction.Builder delete(String brokerName) {
            DeleteBrokerAction.Builder builder = new DeleteBrokerAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .name(brokerName);
            delegate = builder;
            return builder;
        }

        /**
         * Verify given broker instance is running.
         * @param brokerName the name of the Knative broker.
         */
        public VerifyBrokerAction.Builder verify(String brokerName) {
            VerifyBrokerAction.Builder builder = new VerifyBrokerAction.Builder()
                    .client(kubernetesClient)
                    .client(knativeClient)
                    .name(brokerName);
            delegate = builder;
            return builder;
        }
    }
}
