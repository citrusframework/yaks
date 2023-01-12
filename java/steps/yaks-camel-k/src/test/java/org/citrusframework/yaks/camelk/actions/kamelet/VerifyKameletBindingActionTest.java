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
import org.springframework.core.io.ClassPathResource;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

public class VerifyKameletBindingActionTest {

    private final KubernetesMockServer k8sServer = new KubernetesMockServer(new Context(), new MockWebServer(),
            new HashMap<>(), new KubernetesCrudDispatcher(), false);

    private final KubernetesClient kubernetesClient = k8sServer.createClient();

    private final TestContext context = TestContextFactory.newInstance().getObject();

    private static Path sampleBinding;

    @BeforeClass
    public static void setup() throws IOException {
        sampleBinding = new ClassPathResource("timer-to-log-binding.yaml").getFile().toPath();
        camel().version();
    }

    @Test
    public void shouldVerifyLocalJBangIntegration() {
        ProcessAndOutput pao = camel().run("timer-to-log-binding", sampleBinding);
        Long pid = pao.getCamelProcessId();

        try {
            VerifyKameletBindingAction action = new VerifyKameletBindingAction.Builder()
                    .client(kubernetesClient)
                    .isAvailable("timer-to-log-binding")
                    .clusterType(YaksClusterType.LOCAL)
                    .maxAttempts(10)
                    .build();

            context.setVariable("timer-to-log-binding:pid", pid);
            context.setVariable("timer-to-log-binding:process:" + pid, pao);

            action.execute(context);
        } finally {
            camel().stop(pid);
        }
    }
}