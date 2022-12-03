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

package org.citrusframework.yaks.camelk.actions.kamelet;

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
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import static org.awaitility.Awaitility.await;
import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

public class VerifyKameletBindingActionTest {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(VerifyKameletBindingActionTest.class);

    private final KubernetesMockServer k8sServer = new KubernetesMockServer(new Context(), new MockWebServer(),
            new HashMap<>(), new KubernetesCrudDispatcher(), false);

    private final KubernetesClient kubernetesClient = k8sServer.createClient();

    private final TestContext context = TestContextFactory.newInstance().getObject();

    private static Path sampleBinding;

    @BeforeClass
    public static void setup() throws IOException {
        sampleBinding = new ClassPathResource("timer-to-log-binding.yaml").getFile().toPath();
    }

    @Test
    public void shouldVerifyLocalJBangIntegration() {
        ProcessAndOutput pao = camel().run("timer-to-log-binding.yaml", sampleBinding);
        Long pid = pao.getCamelProcessId();

        try {
            VerifyKameletBindingAction action = new VerifyKameletBindingAction.Builder()
                    .client(kubernetesClient)
                    .isAvailable("timer-to-log-binding.yaml")
                    .clusterType(YaksClusterType.LOCAL)
                    .build();

            context.setVariable("timer-to-log-binding.yaml:pid", pid);
            context.setVariable("timer-to-log-binding.yaml:process:" + pid, pao);

            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> !camel().get(pid).isEmpty());

            action.execute(context);
        } finally {
            camel().stop(pid);
        }
    }
}