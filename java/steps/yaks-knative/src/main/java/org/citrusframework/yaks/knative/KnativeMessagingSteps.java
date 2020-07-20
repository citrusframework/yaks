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

package org.citrusframework.yaks.knative;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.knative.messaging.v1alpha1.Channel;
import io.fabric8.knative.messaging.v1alpha1.ChannelBuilder;
import io.fabric8.knative.messaging.v1alpha1.Subscription;
import io.fabric8.knative.messaging.v1alpha1.SubscriptionBuilder;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.knative.actions.KnativeTestAction;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;

/**
 * @author Christoph Deppisch
 */
public class KnativeMessagingSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    @Before
    public void before(Scenario scenario) {
        if (k8sClient == null) {
            k8sClient = KnativeSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^create Knative channel ([^\\s]+)$")
    public void createChannel(String channelName) {
        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Channel channel = new ChannelBuilder()
                        .withApiVersion("messaging.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(channelName)
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .build();

                KnativeSupport.createResource(k8sClient, namespace(context),
                        KnativeSupport.messagingCRDContext("channels"), channel);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.messagingCRDContext("channels"), channelName);
                                }
                            }
                    ));
        }
    }

    @Given("^subscribe service ([^\\s]+) to Knative channel ([^\\s]+)$")
    public void createSubscription(String serviceName, String channelName) {
        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Subscription subscription = new SubscriptionBuilder()
                        .withApiVersion("messaging.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(serviceName + "-subscription")
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .withNewSpec()
                            .withChannel(new ObjectReferenceBuilder()
                                    .withApiVersion("messaging.knative.dev/v1")
                                    .withKind("InMemoryChannel")
                                    .withName(channelName)
                                    .build())
                            .withNewSubscriber()
                                .withNewRef()
                                    .withApiVersion("v1")
                                    .withKind("Service")
                                    .withName(serviceName)
                                .endRef()
                            .endSubscriber()
                        .endSpec()
                        .build();

                KnativeSupport.createResource(k8sClient, namespace(context),
                        KnativeSupport.messagingCRDContext("subscriptions"), subscription);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.messagingCRDContext("subscriptions"), serviceName + "-subscription");
                                }
                            }
                    ));
        }
    }
}
