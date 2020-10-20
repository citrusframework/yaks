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
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

import static com.consol.citrus.container.FinallySequence.Builder.doFinally;
import static org.citrusframework.yaks.knative.actions.KnativeActionBuilder.knative;

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
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^create Knative channel ([^\\s]+)$")
    public void createChannel(String channelName) {
        runner.given(knative().client(k8sClient)
                .createChannel(channelName));

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient).deleteChannel(channelName)));
        }
    }

    @Given("^subscribe service ([^\\s]+) to Knative channel ([^\\s]+)$")
    public void createSubscription(String serviceName, String channelName) {
        runner.given(knative().client(k8sClient)
                .createSubscription(serviceName + "subscription")
                .onChannel(channelName)
                .service(serviceName));

        if (KnativeSettings.isAutoRemoveResources()) {
            runner.then(doFinally()
                    .actions(knative().client(k8sClient).deleteSubscription(serviceName + "subscription")));
        }
    }
}
