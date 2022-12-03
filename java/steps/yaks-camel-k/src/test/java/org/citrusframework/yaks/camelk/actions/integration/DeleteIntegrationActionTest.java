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
import java.util.concurrent.TimeUnit;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.context.TestContextFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesCrudDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import okhttp3.mockwebserver.MockWebServer;
import org.citrusframework.yaks.YaksClusterType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.awaitility.Awaitility.await;
import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

public class DeleteIntegrationActionTest {

    private final KubernetesMockServer k8sServer = new KubernetesMockServer(new Context(), new MockWebServer(),
            new HashMap<>(), new KubernetesCrudDispatcher(), false);

    private final KubernetesClient kubernetesClient = k8sServer.createClient();

    private final TestContext context = TestContextFactory.newInstance().getObject();

    private static Path sampleIntegration;

    @BeforeClass
    public static void setup() throws IOException {
        sampleIntegration = new ClassPathResource("simple.groovy").getFile().toPath();
    }

    @Test
    public void shouldDeleteLocalJBangIntegration() {
        Long pid = camel().run("simple.groovy", sampleIntegration).getCamelProcessId();

        try {
            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> !camel().get(pid).isEmpty());

            DeleteIntegrationAction action = new DeleteIntegrationAction.Builder()
                    .client(kubernetesClient)
                    .integration("simple")
                    .clusterType(YaksClusterType.LOCAL)
                    .build();

            context.setVariable("simple:pid", pid);

            action.execute(context);

            await().atMost(15000L, TimeUnit.MILLISECONDS).until(() -> camel().get(pid).isEmpty());
        } finally {
            camel().stop(pid);
        }
    }

    @Test
    public void shouldDeleteLocalJBangIntegrationByName() {
        Long pid = camel().run("simple.groovy", sampleIntegration).getCamelProcessId();

        try {
            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> !camel().get(pid).isEmpty());

            DeleteIntegrationAction action = new DeleteIntegrationAction.Builder()
                    .client(kubernetesClient)
                    .integration("simple.groovy")
                    .clusterType(YaksClusterType.LOCAL)
                    .build();

            action.execute(context);

            await().atMost(15000L, TimeUnit.MILLISECONDS).until(() -> camel().get(pid).isEmpty());
        } finally {
            camel().stop(pid);
        }
    }
}