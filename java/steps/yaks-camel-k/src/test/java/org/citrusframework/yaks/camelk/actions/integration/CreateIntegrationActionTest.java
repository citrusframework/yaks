/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.camelk.actions.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesCrudDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.camel.v1.Integration;
import org.citrusframework.context.TestContext;
import org.citrusframework.context.TestContextFactory;
import org.citrusframework.yaks.YaksClusterType;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.citrusframework.yaks.kubernetes.KubernetesSettings;
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
                .traits("quarkus.enabled=true,quarkus.nativeBaseImage=java-native,route.enabled=true,openapi.configmaps=[spec]")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("helloworld").get();
        Assert.assertNotNull(integration.getSpec().getTraits().getQuarkus());
        Assert.assertTrue(integration.getSpec().getTraits().getQuarkus().getEnabled());
        Assert.assertEquals("java-native", integration.getSpec().getTraits().getQuarkus().getNativeBaseImage());
        Assert.assertNotNull(integration.getSpec().getTraits().getRoute());
        Assert.assertTrue(integration.getSpec().getTraits().getRoute().getEnabled());
        Assert.assertNotNull(integration.getSpec().getTraits().getOpenapi());
        Assert.assertEquals(1L, integration.getSpec().getTraits().getOpenapi().getConfigmaps().size());
        Assert.assertEquals("spec", integration.getSpec().getTraits().getOpenapi().getConfigmaps().get(0));
    }

    @Test
    public void shouldCreateIntegrationWithTraitModeline() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: trait=quarkus.enabled=true\n" +
                        "// camel-k: trait=quarkus.nativeBaseImage=native-java\n" +
                        "// camel-k: trait=environment.vars=[foo=bar,baz=foobar]\n" +
                        "// camel-k: trait=mount.configs=secret:foo,configmap:bar\n" +
                        "// camel-k: trait=mount.resources=\"secret:foo-resource\",\"configmap:bar-resource\"\n" +
                        "// camel-k: trait=route.enabled=true\n" +
                        "// camel-k: trait=container.port=8443\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("foo").get();
        Assert.assertNotNull(integration.getSpec().getTraits().getEnvironment());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getEnvironment().getVars().size());
        Assert.assertEquals("foo=bar", integration.getSpec().getTraits().getEnvironment().getVars().get(0));
        Assert.assertEquals("baz=foobar", integration.getSpec().getTraits().getEnvironment().getVars().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getMount());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getMount().getConfigs().size());
        Assert.assertEquals("secret:foo", integration.getSpec().getTraits().getMount().getConfigs().get(0));
        Assert.assertEquals("configmap:bar", integration.getSpec().getTraits().getMount().getConfigs().get(1));
        Assert.assertEquals(2L, integration.getSpec().getTraits().getMount().getResources().size());
        Assert.assertEquals("secret:foo-resource", integration.getSpec().getTraits().getMount().getResources().get(0));
        Assert.assertEquals("configmap:bar-resource", integration.getSpec().getTraits().getMount().getResources().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getQuarkus());
        Assert.assertTrue(integration.getSpec().getTraits().getQuarkus().getEnabled());
        Assert.assertEquals("native-java", integration.getSpec().getTraits().getQuarkus().getNativeBaseImage());
        Assert.assertNotNull(integration.getSpec().getTraits().getRoute());
        Assert.assertTrue(integration.getSpec().getTraits().getRoute().getEnabled());
        Assert.assertEquals(8443L, integration.getSpec().getTraits().getContainer().getPort().longValue());
    }

    @Test
    public void shouldCreateIntegrationWithTraitModelineShortcuts() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: property=p1=foo\n" +
                        "// camel-k: property=p2=foobar\n" +
                        "// camel-k: build-property=b1=foo\n" +
                        "// camel-k: build-property=b2=bar\n" +
                        "// camel-k: env=foo=bar\n" +
                        "// camel-k: env=baz=foobar\n" +
                        "// camel-k: connect=service1\n" +
                        "// camel-k: connect=service2\n" +
                        "// camel-k: volume=v1\n" +
                        "// camel-k: volume=v2\n" +
                        "// camel-k: open-api=configmap:foo-spec\n" +
                        "// camel-k: dependency=camel:jackson\n" +
                        "// camel-k: dependency=camel:jackson-avro\n" +
                        "// camel-k: config=secret:foo\n" +
                        "// camel-k: config=configmap:bar\n" +
                        "// camel-k: resource=\"secret:foo-resource\"\n" +
                        "// camel-k: resource=\"configmap:bar-resource\"\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("foo").get();
        Assert.assertNotNull(integration.getSpec().getDependencies());
        Assert.assertEquals(2L, integration.getSpec().getDependencies().size());
        Assert.assertEquals("camel:jackson", integration.getSpec().getDependencies().get(0));
        Assert.assertEquals("camel:jackson-avro", integration.getSpec().getDependencies().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getServiceBinding());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getServiceBinding().getServices().size());
        Assert.assertEquals("service1", integration.getSpec().getTraits().getServiceBinding().getServices().get(0));
        Assert.assertEquals("service2", integration.getSpec().getTraits().getServiceBinding().getServices().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getOpenapi());
        Assert.assertEquals(1L, integration.getSpec().getTraits().getOpenapi().getConfigmaps().size());
        Assert.assertEquals("configmap:foo-spec", integration.getSpec().getTraits().getOpenapi().getConfigmaps().get(0));
        Assert.assertNotNull(integration.getSpec().getTraits().getEnvironment());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getEnvironment().getVars().size());
        Assert.assertEquals("foo=bar", integration.getSpec().getTraits().getEnvironment().getVars().get(0));
        Assert.assertEquals("baz=foobar", integration.getSpec().getTraits().getEnvironment().getVars().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getMount());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getMount().getVolumes().size());
        Assert.assertEquals("v1", integration.getSpec().getTraits().getMount().getVolumes().get(0));
        Assert.assertEquals("v2", integration.getSpec().getTraits().getMount().getVolumes().get(1));
        Assert.assertEquals(2L, integration.getSpec().getTraits().getMount().getConfigs().size());
        Assert.assertEquals("secret:foo", integration.getSpec().getTraits().getMount().getConfigs().get(0));
        Assert.assertEquals("configmap:bar", integration.getSpec().getTraits().getMount().getConfigs().get(1));
        Assert.assertEquals(2L, integration.getSpec().getTraits().getMount().getResources().size());
        Assert.assertEquals("secret:foo-resource", integration.getSpec().getTraits().getMount().getResources().get(0));
        Assert.assertEquals("configmap:bar-resource", integration.getSpec().getTraits().getMount().getResources().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getCamel());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getCamel().getProperties().size());
        Assert.assertEquals("p1=foo", integration.getSpec().getTraits().getCamel().getProperties().get(0));
        Assert.assertEquals("p2=foobar", integration.getSpec().getTraits().getCamel().getProperties().get(1));
        Assert.assertNotNull(integration.getSpec().getTraits().getBuilder());
        Assert.assertEquals(2L, integration.getSpec().getTraits().getBuilder().getProperties().size());
        Assert.assertEquals("b1=foo", integration.getSpec().getTraits().getBuilder().getProperties().get(0));
        Assert.assertEquals("b2=bar", integration.getSpec().getTraits().getBuilder().getProperties().get(1));
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

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("helloworld").get();
        Assert.assertNotNull(integration.getSpec().getTraits().getBuilder());
        List<String> values = integration.getSpec().getTraits().getBuilder().getProperties();
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

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("foo").get();
        Assert.assertNotNull(integration.getSpec().getTraits().getBuilder());
        List<String> values = integration.getSpec().getTraits().getBuilder().getProperties();
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("quarkus.foo=bar", values.get(0));
        Assert.assertEquals("quarkus.verbose=true", values.get(1));
    }

    @Test
    public void shouldCreateIntegrationWithEnvVars() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("helloworld")
                .fileName("helloworld.groovy")
                .source("from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .envVar("QUARKUS_FOO", "bar")
                .envVar("QUARKUS_VERBOSE", "true")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("helloworld").get();
        Assert.assertNotNull(integration.getSpec().getTraits().getEnvironment());
        List<String> values = integration.getSpec().getTraits().getEnvironment().getVars();
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("QUARKUS_FOO=bar", values.get(0));
        Assert.assertEquals("QUARKUS_VERBOSE=true", values.get(1));
    }

    @Test
    public void shouldCreateIntegrationWithEnvVarModeline() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: env=QUARKUS_FOO=bar\n" +
                        "// camel-k: env=QUARKUS_VERBOSE=true\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("foo").get();
        Assert.assertNotNull(integration.getSpec().getTraits().getEnvironment());
        List<String> values = integration.getSpec().getTraits().getEnvironment().getVars();
        Assert.assertEquals(2, values.size());
        Assert.assertEquals("QUARKUS_FOO=bar", values.get(0));
        Assert.assertEquals("QUARKUS_VERBOSE=true", values.get(1));
    }

    @Test
    public void shouldCreateIntegrationWithConfigModeline() {
        CreateIntegrationAction action = new CreateIntegrationAction.Builder()
                .client(kubernetesClient)
                .integration("foo")
                .fileName("foo.groovy")
                .source("// camel-k: config=secret:my-secret\n" +
                        "// camel-k: config=configmap:tokens\n" +
                        "// camel-k: property=foo=bar\n" +
                        "from('timer:tick?period=1000').setBody().constant('Hello world from Camel K!').to('log:info')")
                .build();

        action.execute(context);

        Integration integration = kubernetesClient.resources(Integration.class).inNamespace(KubernetesSettings.getNamespace()).withName("foo").get();
        Assert.assertEquals(2, integration.getSpec().getTraits().getMount().getConfigs().size());
        Assert.assertEquals("secret:my-secret", integration.getSpec().getTraits().getMount().getConfigs().get(0));
        Assert.assertEquals("configmap:tokens", integration.getSpec().getTraits().getMount().getConfigs().get(1));
        Assert.assertEquals("foo=bar", integration.getSpec().getTraits().getCamel().getProperties().get(0));
    }

    @Test
    public void shouldCreateLocalIntegration() {
        camel().version();
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

            Assert.assertTrue(pao.getProcess().isAlive());
        } finally {
            camel().stop(pid);
        }
    }
}
