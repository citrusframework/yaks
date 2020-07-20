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

import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.knative.eventing.v1alpha1.Broker;
import io.fabric8.knative.eventing.v1alpha1.BrokerBuilder;
import io.fabric8.knative.eventing.v1alpha1.Trigger;
import io.fabric8.knative.eventing.v1alpha1.TriggerBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.citrusframework.yaks.knative.actions.KnativeTestAction;

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static com.consol.citrus.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;

/**
 * @author Christoph Deppisch
 */
public class KnativeEventingSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    private String brokerName = KnativeSettings.getBrokerName();

    @Before
    public void before(Scenario scenario) {
        // Use given namespace by initializing a test variable in the test runner. Other test actions and steps
        // may use the variable as expression or resolve the variable value via test context.
        runner.variable(KnativeVariableNames.BROKER_NAME.value(), brokerName);

        if (k8sClient == null) {
            k8sClient = KnativeSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^Knative broker ([^\\s]+)$")
    public void useBroker(String brokerName) {
        setBrokerName(brokerName);
    }

    @Given("^create Knative broker ([^\\s]+)$")
    public void createBroker(String brokerName) {
        setBrokerName(brokerName);

        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Broker broker = new BrokerBuilder()
                        .withApiVersion("eventing.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(brokerName)
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .build();

                KnativeSupport.createResource(k8sClient, namespace(context),
                        KnativeSupport.eventingCRDContext("brokers"), broker);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.eventingCRDContext("brokers"), brokerName);
                                }
                            }
                    ));
        }
    }

    @Given("^Knative broker ([^\\s]+) is running$")
    public void verifyBrokerIsRunning(String brokerName) {
        runner.then(repeatOnError()
            .autoSleep(500)
            .until((i, context) -> i == 10)
            .actions(
                new KnativeTestAction() {
                    @Override
                    public void doExecute(TestContext context) {
                        try {
                            Map<String, Object> resources = k8sClient.customResource(KnativeSupport.eventingCRDContext("brokers"))
                                    .get(namespace(context), brokerName);

                            Broker broker = KnativeSupport.json()
                                    .reader()
                                    .forType(Broker.class)
                                    .readValue(KnativeSupport.json().writeValueAsString(resources));

                            if (broker.getStatus() != null &&
                                broker.getStatus().getConditions() != null &&
                                broker.getStatus().getConditions().stream()
                                    .anyMatch(condition -> condition.getType().equals("Ready") && condition.getStatus().equals("True"))) {
                                LOG.info(String.format("Knative broker %s is ready", brokerName));
                            } else {
                                throw new ValidationException(String.format("Knative broker '%s' is not ready", brokerName));
                            }
                        } catch (JsonProcessingException e) {
                            throw new ValidationException(String.format("Failed to validate Knative broker '%s' state", brokerName), e);
                        } catch (KubernetesClientException e) {
                            throw new ValidationException(String.format("Failed to validate Knative broker '%s' - " +
                                    "not found in namespace '%s'", brokerName, namespace(context)), e);
                        }
                    }
                }
            ));
    }

    @Given("^create Knative trigger ([^\\s]+) on service ([^\\s]+)$")
    public void createTriggerOnService(String triggerName, String serviceName) {
        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Trigger trigger = new TriggerBuilder()
                        .withApiVersion("eventing.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(triggerName)
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .withNewSpec()
                            .withBroker(brokerName(context))
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
                        KnativeSupport.eventingCRDContext("triggers"), trigger);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.eventingCRDContext("triggers"), triggerName);
                                }
                            }
                    ));
        }
    }

    @Given("^create Knative trigger ([^\\s]+) on service ([^\\s]+) with filter on attributes$")
    public void createTriggerOnServiceFiltered(String triggerName, String serviceName, DataTable filterAttributes) {
        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Trigger trigger = new TriggerBuilder()
                        .withApiVersion("eventing.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(triggerName)
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .withNewSpec()
                            .withBroker(brokerName(context))
                            .withNewFilter()
                                .withAttributes(filterAttributes.asMap(String.class, String.class))
                            .endFilter()
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
                        KnativeSupport.eventingCRDContext("triggers"), trigger);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.eventingCRDContext("triggers"), triggerName);
                                }
                            }
                    ));
        }
    }

    @Given("^create Knative trigger ([^\\s]+) on channel ([^\\s]+)$")
    public void createTriggerOnChannel(String triggerName, String channelName) {
        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Trigger trigger = new TriggerBuilder()
                        .withApiVersion("eventing.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(triggerName)
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .withNewSpec()
                            .withBroker(brokerName(context))
                            .withNewSubscriber()
                                .withNewRef()
                                    .withApiVersion("messaging.knative.dev/v1")
                                    .withKind("InMemoryChannel")
                                    .withName(channelName)
                                .endRef()
                            .endSubscriber()
                        .endSpec()
                        .build();

                KnativeSupport.createResource(k8sClient, namespace(context),
                        KnativeSupport.eventingCRDContext("triggers"), trigger);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.eventingCRDContext("triggers"), triggerName);
                                }
                            }
                    ));
        }
    }

    @Given("^create Knative trigger ([^\\s]+) on channel ([^\\s]+) with filter on attributes$")
    public void createTriggerFiltered(String triggerName, String channelName, DataTable filterAttributes) {
        runner.given(new KnativeTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Trigger trigger = new TriggerBuilder()
                        .withApiVersion("eventing.knative.dev/v1")
                        .withNewMetadata()
                            .withNamespace(namespace(context))
                            .withName(triggerName)
                            .withLabels(KnativeSettings.getDefaultLabels())
                        .endMetadata()
                        .withNewSpec()
                            .withBroker(brokerName(context))
                            .withNewFilter()
                                .withAttributes(filterAttributes.asMap(String.class, String.class))
                            .endFilter()
                            .withNewSubscriber()
                                .withNewRef()
                                    .withApiVersion("messaging.knative.dev/v1")
                                    .withKind("InMemoryChannel")
                                    .withName(channelName)
                                .endRef()
                            .endSubscriber()
                        .endSpec()
                        .build();

                KnativeSupport.createResource(k8sClient, namespace(context),
                        KnativeSupport.eventingCRDContext("triggers"), trigger);
            }
        });

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(
                            new KnativeTestAction() {
                                @Override
                                public void doExecute(TestContext context) {
                                    KnativeSupport.deleteResource(k8sClient, namespace(context),
                                            KnativeSupport.eventingCRDContext("triggers"), triggerName);
                                }
                            }
                    ));
        }
    }

    private void setBrokerName(String brokerName) {
        this.brokerName = brokerName;

        // update the test variable that points to the broker name
        runner.run(createVariable(KnativeVariableNames.BROKER_NAME.value(), brokerName));
    }

}
