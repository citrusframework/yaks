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
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;
import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static com.consol.citrus.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.yaks.knative.actions.KnativeActionBuilder.knative;

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
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^Knative broker ([^\\s]+)$")
    public void useBroker(String brokerName) {
        setBrokerName(brokerName);
    }

    @Given("^create Knative broker ([^\\s]+)$")
    public void createBroker(String brokerName) {
        setBrokerName(brokerName);

        runner.given(knative().client(k8sClient)
                .brokers()
                .create(brokerName));

        if (KnativeSteps.autoRemoveResources) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient)
                            .brokers()
                            .delete(brokerName)));
        }
    }

    @Given("^Knative broker ([^\\s]+) is running$")
    public void verifyBrokerIsRunning(String brokerName) {
        runner.then(repeatOnError()
            .autoSleep(500)
            .until((i, context) -> i == 10)
            .actions(knative().client(k8sClient)
                    .brokers()
                    .verify(brokerName)));
    }

    @Given("^create Knative trigger ([^\\s]+) on service ([^\\s]+)$")
    public void createTriggerOnService(String triggerName, String serviceName) {
        runner.given(knative().client(k8sClient)
                .trigger()
                .create(triggerName)
                .onService(serviceName));

        if (KnativeSteps.autoRemoveResources) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient)
                            .trigger()
                            .delete(triggerName)));
        }
    }

    @Given("^create Knative trigger ([^\\s]+) on service ([^\\s]+) with filter on attributes$")
    public void createTriggerOnServiceFiltered(String triggerName, String serviceName, DataTable filterAttributes) {
        runner.given(knative().client(k8sClient)
                .trigger()
                .create(triggerName)
                .onService(serviceName)
                .filter(filterAttributes.asMap(String.class, String.class)));

        if (KnativeSteps.autoRemoveResources) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient)
                            .trigger()
                            .delete(triggerName)));
        }
    }

    @Given("^create Knative trigger ([^\\s]+) on channel ([^\\s]+)$")
    public void createTriggerOnChannel(String triggerName, String channelName) {
        runner.given(knative().client(k8sClient)
                .trigger()
                .create(triggerName)
                .onChannel(channelName));

        if (KnativeSteps.autoRemoveResources) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient)
                            .trigger()
                            .delete(triggerName)));
        }
    }

    @Given("^create Knative trigger ([^\\s]+) on channel ([^\\s]+) with filter on attributes$")
    public void createTriggerFiltered(String triggerName, String channelName, DataTable filterAttributes) {
        runner.given(knative().client(k8sClient)
                .trigger()
                .create(triggerName)
                .onChannel(channelName)
                .filter(filterAttributes.asMap(String.class, String.class)));

        if (KnativeSteps.autoRemoveResources) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient)
                            .trigger()
                            .delete(triggerName)));
        }
    }

    private void setBrokerName(String brokerName) {
        this.brokerName = brokerName;

        // update the test variable that points to the broker name
        runner.run(createVariable(KnativeVariableNames.BROKER_NAME.value(), brokerName));
    }

}
