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

import static com.consol.citrus.actions.CreateVariablesAction.Builder.createVariable;

/**
 * @author Christoph Deppisch
 */
public class KnativeSteps {

    @CitrusResource
    private TestCaseRunner runner;

    private String namespace = KnativeSettings.getNamespace();

    @CitrusFramework
    private Citrus citrus;

    private KubernetesClient k8sClient;

    @Before
    public void before(Scenario scenario) {
        // Use given namespace by initializing a test variable in the test runner. Other test actions and steps
        // may use the variable as expression or resolve the variable value via test context.
        runner.variable(KnativeVariableNames.NAMESPACE.value(), namespace);

        if (k8sClient == null) {
            k8sClient = KubernetesSupport.getKubernetesClient(citrus);
        }
    }

    @Given("^Knative namespace ([^\\s]+)$")
    public void setNamespace(String namespace) {
        this.namespace = namespace;

        // update the test variable that points to the namespace
        runner.run(createVariable(KnativeVariableNames.NAMESPACE.value(), namespace));
    }
}
