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

package org.citrusframework.yaks.hooks;

import java.util.Optional;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.YaksVariableNames;
import org.citrusframework.yaks.util.CucumberUtils;

/**
 * Cucumber hook injects environment variables as test variables before the scenario is executed.
 * @author Christoph Deppisch
 */
public class InjectEnvVarsHook {

    @CitrusResource
    private TestCaseRunner runner;

    @Before(order = Integer.MAX_VALUE)
    public void injectEnvVars(Scenario scenario) {
        runner.run(new AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                if (scenario != null) {
                    context.setVariable(YaksVariableNames.FEATURE_FILE.value(), CucumberUtils.extractFeatureFileName(scenario));
                    context.setVariable(YaksVariableNames.SCENARIO_ID.value(), scenario.getId());
                    context.setVariable(YaksVariableNames.SCENARIO_NAME.value(), scenario.getName());
                }

                Optional<String> namespaceEnv = getNamespaceSetting();
                Optional<String> domainEnv = getClusterWildcardSetting();

                if (namespaceEnv.isPresent()) {
                    context.setVariable(YaksVariableNames.NAMESPACE.value(), namespaceEnv.get());

                    if (!domainEnv.isPresent()) {
                        context.setVariable(YaksVariableNames.CLUSTER_WILDCARD_DOMAIN.value(), namespaceEnv.get() + "." + YaksSettings.DEFAULT_DOMAIN_SUFFIX);
                    }
                }

                domainEnv.ifPresent(var -> context.setVariable(YaksVariableNames.CLUSTER_WILDCARD_DOMAIN.value(), var));

                context.setVariable(YaksVariableNames.OPERATOR_NAMESPACE.value(), YaksSettings.getOperatorNamespace());
            }
        });
    }

    protected Optional<String> getClusterWildcardSetting() {
        return Optional.ofNullable(YaksSettings.getClusterWildcardDomain());
    }

    protected Optional<String> getNamespaceSetting() {
        return Optional.ofNullable(YaksSettings.getDefaultNamespace());
    }
}
