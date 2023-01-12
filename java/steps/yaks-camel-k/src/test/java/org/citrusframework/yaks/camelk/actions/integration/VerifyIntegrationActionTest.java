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

package org.citrusframework.yaks.camelk.actions.integration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.context.TestContextFactory;
import com.consol.citrus.exceptions.ActionTimeoutException;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesCrudDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import okhttp3.mockwebserver.MockWebServer;
import org.citrusframework.yaks.YaksClusterType;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

public class VerifyIntegrationActionTest {

    private final KubernetesMockServer k8sServer = new KubernetesMockServer(new Context(), new MockWebServer(),
            new HashMap<>(), new KubernetesCrudDispatcher(), false);

    private final KubernetesClient kubernetesClient = k8sServer.createClient();

    private final TestContext context = TestContextFactory.newInstance().getObject();

    private static Path sampleIntegration;

    @BeforeClass
    public static void setup() throws IOException {
        sampleIntegration = new ClassPathResource("simple.groovy").getFile().toPath();
        camel().version();
    }

    @Test
    public void shouldVerifyLocalJBangIntegration() {
        ProcessAndOutput pao = camel().run("simple", sampleIntegration);
        Long pid = pao.getCamelProcessId();

        try {
            VerifyIntegrationAction action = new VerifyIntegrationAction.Builder()
                    .client(kubernetesClient)
                    .integrationName("simple")
                    .clusterType(YaksClusterType.LOCAL)
                    .maxAttempts(10)
                    .build();

            context.setVariable("simple:pid", pid);
            context.setVariable("simple:process:" + pid, pao);

            action.execute(context);
        } finally {
            camel().stop(pid);
        }
    }

    @Test
    public void shouldVerifyLocalJBangIntegrationLogs() {
        ProcessAndOutput pao = camel().run("simple", sampleIntegration);
        Long pid = pao.getCamelProcessId();

        try {
            VerifyIntegrationAction action = new VerifyIntegrationAction.Builder()
                    .client(kubernetesClient)
                    .integrationName("simple")
                    .waitForLogMessage("Hello from Camel K!")
                    .clusterType(YaksClusterType.LOCAL)
                    .maxAttempts(10)
                    .delayBetweenAttempts(2000L)
                    .build();

            context.setVariable("simple:pid", pid);
            context.setVariable("simple:process:" + pid, pao);

            action.execute(context);

            action = new VerifyIntegrationAction.Builder()
                    .client(kubernetesClient)
                    .integrationName("simple")
                    .waitForLogMessage("Now something completely different!")
                    .maxAttempts(10)
                    .delayBetweenAttempts(2000L)
                    .clusterType(YaksClusterType.LOCAL)
                    .build();

            try {
                action.execute(context);
                Assert.fail("Missing integration logs verification exception");
            } catch (ActionTimeoutException e) {
                Assert.assertEquals("Failed to verify integration 'simple' - " +
                        "has not printed message 'Now something completely different!' after 10 attempts", e.getCause().getMessage());
            }
        } finally {
            camel().stop(pid);
        }
    }
}