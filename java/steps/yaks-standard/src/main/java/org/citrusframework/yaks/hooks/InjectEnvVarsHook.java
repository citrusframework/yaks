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

import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.context.TestContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber hook injects environment variables as test variables before the scenario is executed.
 * @author Christoph Deppisch
 */
public class InjectEnvVarsHook {

    public static final String DEFAULT_DOMAIN_SUFFIX = ".svc.cluster.local";
    public static final String CLUSTER_WILDCARD_DOMAIN = "CLUSTER_WILDCARD_DOMAIN";
    public static final String YAKS_NAMESPACE = "YAKS_NAMESPACE";

    @CitrusResource
    private TestCaseRunner runner;

    @Before
    public void injectEnvVars(Scenario scenario) {
        runner.run(new AbstractTestAction() {
            @Override
            public void doExecute(TestContext context) {
                if (scenario != null) {
                    context.setVariable("SCENARIO_ID", scenario.getId());
                    context.setVariable("SCENARIO_NAME", scenario.getName());
                }

                Optional<String> namespaceEnv = getEnvSetting(YAKS_NAMESPACE);
                Optional<String> domainEnv = getEnvSetting(CLUSTER_WILDCARD_DOMAIN);

                if (namespaceEnv.isPresent()) {
                    context.setVariable(YAKS_NAMESPACE, namespaceEnv.get());

                    if (!domainEnv.isPresent()) {
                        context.setVariable(CLUSTER_WILDCARD_DOMAIN, namespaceEnv.get() + DEFAULT_DOMAIN_SUFFIX);
                    }
                }

                domainEnv.ifPresent(var -> context.setVariable(CLUSTER_WILDCARD_DOMAIN, var));
            }
        });
    }

    /**
     * Read environment setting. If setting is not present default to empty value.
     * @param name
     * @return
     */
    protected Optional<String> getEnvSetting(String name) {
        return Optional.ofNullable(System.getenv(name));
    }
}
