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

package org.citrusframework.yaks.kubernetes;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.assertj.core.api.Assertions;
import org.citrusframework.yaks.kubernetes.actions.KubernetesAction;

/**
 * @author Christoph Deppisch
 */
public class KubernetesTestSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    @Then("^verify Kubernetes service ([^\\s]+) exists$")
    public void verifyService(String serviceName) {
        runner.run(new KubernetesTestAction() {
            @Override
            public void doExecute(TestContext context) {
                Assertions.assertThat(KubernetesSupport.getKubernetesClient(citrus)
                        .services()
                        .inNamespace(namespace(context))
                        .withName(serviceName)
                        .get()).isNotNull();
            }
        });
    }

    private abstract class KubernetesTestAction extends AbstractTestAction implements KubernetesAction {
        @Override
        public KubernetesClient getKubernetesClient() {
            return KubernetesSupport.getKubernetesClient(citrus);
        }
    }
}
