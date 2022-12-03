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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.citrusframework.yaks.camelk.model.Integration;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.awaitility.Awaitility.await;
import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * @author Christoph Deppisch
 */
public class CreateIntegrationActionTest {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CreateIntegrationActionTest.class);

    private final KubernetesMockServer k8sServer = new KubernetesMockServer(new Context(), new MockWebServer(),
            new HashMap<>(), new KubernetesCrudDispatcher(), false);

    private final KubernetesClient kubernetesClient = k8sServer.createClient();

    private final TestContext context = TestContextFactory.newInstance().getObject();

    @Test
    public void shouldCreateIntegrationWithTraits() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("helloworld")
                .source("from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .traits("quarkus.enabled=true,quarkus.native=true,route.enabled=true")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).withName("helloworld").get();
        Assert.assertTrue(integration.getSpec().getTraits().containsKey("quarkus"));
        Assert.assertEquals(2, integration.getSpec().getTraits().get("quarkus").getConfiguration().size());
        Assert.assertEquals(true, integration.getSpec().getTraits().get("quarkus").getConfiguration().get("enabled"));
        Assert.assertEquals("true", integration.getSpec().getTraits().get("quarkus").getConfiguration().get("native"));
        Assert.assertTrue(integration.getSpec().getTraits().containsKey("route"));
        Assert.assertEquals(1, integration.getSpec().getTraits().get("route").getConfiguration().size());
        Assert.assertEquals(true, integration.getSpec().getTraits().get("route").getConfiguration().get("enabled"));
    }

    @Test
    public void shouldCreateIntegrationWithTraitModeline() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: trait=quarkus.enabled=true\n" +
                        "// camel-k: trait=quarkus.native=true\n" +
                        "// camel-k: trait=route.enabled=true\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).withName("foo").get();
        Assert.assertTrue(integration.getSpec().getTraits().containsKey("quarkus"));
        Assert.assertEquals(2, integration.getSpec().getTraits().get("quarkus").getConfiguration().size());
        Assert.assertEquals(true, integration.getSpec().getTraits().get("quarkus").getConfiguration().get("enabled"));
        Assert.assertEquals("true", integration.getSpec().getTraits().get("quarkus").getConfiguration().get("native"));
        Assert.assertTrue(integration.getSpec().getTraits().containsKey("route"));
        Assert.assertEquals(1, integration.getSpec().getTraits().get("route").getConfiguration().size());
        Assert.assertEquals(true, integration.getSpec().getTraits().get("route").getConfiguration().get("enabled"));
    }

    @Test
    public void shouldCreateIntegrationWithBuildProperties() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("helloworld")
                .fileName("helloworld.groovy")
                .source("from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .buildProperty("quarkus.foo", "bar")
                .buildProperty("quarkus.verbose", "true")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).withName("helloworld").get();
        Assert.assertEquals(1, integration.getSpec().getTraits().size());
        Assert.assertTrue(integration.getSpec().getTraits().containsKey("builder"));
        Assert.assertEquals(1, integration.getSpec().getTraits().get("builder").getConfiguration().size());
        Assert.assertEquals(ArrayList.class, integration.getSpec().getTraits().get("builder").getConfiguration().get("properties").getClass());
        List<String> values = (List<String>) integration.getSpec().getTraits().get("builder").getConfiguration().get("properties");
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("quarkus.foo=bar", values.get(0));
        Assert.assertEquals("quarkus.verbose=true", values.get(1));
    }

    @Test
    public void shouldCreateIntegrationWithBuildPropertyModeline() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: build-property=quarkus.foo=bar\n" +
                        "// camel-k: build-property=quarkus.verbose=true\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).withName("foo").get();
        Assert.assertEquals(1, integration.getSpec().getTraits().size());
        Assert.assertTrue(integration.getSpec().getTraits().containsKey("builder"));
        Assert.assertEquals(1, integration.getSpec().getTraits().get("builder").getConfiguration().size());
        Assert.assertEquals(ArrayList.class, integration.getSpec().getTraits().get("builder").getConfiguration().get("properties").getClass());
        List<String> values = (List<String>) integration.getSpec().getTraits().get("builder").getConfiguration().get("properties");
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("quarkus.foo=bar", values.get(0));
        Assert.assertEquals("quarkus.verbose=true", values.get(1));
    }

    @Test
    public void shouldCreateIntegrationWithConfigModeline() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: config=secret:my-secret\n" +
                        "// camel-k: config=configmap:tokens\n" +
                        "// camel-k: config=foo=bar\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).withName("foo").get();
        Assert.assertEquals(3, integration.getSpec().getConfiguration().size());
        Assert.assertEquals("secret", integration.getSpec().getConfiguration().get(0).getType());
        Assert.assertEquals("configmap", integration.getSpec().getConfiguration().get(1).getType());
        Assert.assertEquals("property", integration.getSpec().getConfiguration().get(2).getType());
    }

    @Test
    public void shouldCreateLocalJBangIntegration() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .clusterType(YaksClusterType.LOCAL)
                .fileName("foo.groovy")
                .source("from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Assert.assertNotNull(context.getVariable("foo:pid"));

        Long pid = context.getVariable("foo:pid", Long.class);

        Assert.assertNotNull(context.getVariable("foo:process:" + pid));
        ProcessAndOutput pao = context.getVariable("foo:process:" + pid, ProcessAndOutput.class);

        try {
            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> {
                Map<String, String> integration = camel().get(pid);

                if (integration.isEmpty() || integration.get("STATUS").equals("Starting")) {
                    LOG.info("Waiting for Camel integration to start ...");
                    return false;
                }

                Assert.assertEquals("foo", integration.get("NAME"));
                Assert.assertEquals("Running", integration.get("STATUS"));

                return true;
            });

            Assert.assertEquals(true, pao.getProcess().isAlive());
        } finally {
            camel().stop(pid);
        }
    }
}
