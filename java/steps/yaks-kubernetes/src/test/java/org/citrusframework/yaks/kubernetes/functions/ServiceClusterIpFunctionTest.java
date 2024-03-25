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

package org.citrusframework.yaks.kubernetes.functions;

import java.util.List;

import org.citrusframework.Citrus;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.functions.DefaultFunctionLibrary;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.junit.Assert;
import org.junit.Test;

public class ServiceClusterIpFunctionTest {

    private final Citrus citrus = Citrus.newInstance();
    private final TestContext context = citrus.getCitrusContext().createTestContext();
    private final KubernetesClient k8sClient = KubernetesSupport.getKubernetesClient(context);

    @Test
    public void shouldResolveServiceClusterIp() {
        Service service = new Service();
        service.setMetadata(new ObjectMeta());
        service.getMetadata().setNamespace(KubernetesSupport.getNamespace(context));
        service.getMetadata().setName("myService");
        service.setSpec(new ServiceSpec());
        service.getSpec().setClusterIP("127.0.0.1");

        k8sClient.services().inNamespace(KubernetesSupport.getNamespace(context))
                .resource(service)
                .create();

        Assert.assertEquals("127.0.0.1", new ServiceClusterIpFunction().execute(List.of("myService"), context));
    }

    @Test
    public void shouldFallbackToExternalIPs() {
        Service service = new Service();
        service.setMetadata(new ObjectMeta());
        service.getMetadata().setNamespace(KubernetesSupport.getNamespace(context));
        service.getMetadata().setName("myExternalService");
        service.setSpec(new ServiceSpec());
        service.getSpec().setExternalIPs(List.of("127.0.0.1"));

        k8sClient.services().inNamespace(KubernetesSupport.getNamespace(context))
                .resource(service)
                .create();

        Assert.assertEquals("127.0.0.1", new ServiceClusterIpFunction().execute(List.of("myExternalService"), context));
    }

    @Test
    public void shouldFailOnUnknownService() {
        Assert.assertThrows(CitrusRuntimeException.class,
                () -> new ServiceClusterIpFunction().execute(List.of("unknown"), context));
    }

    @Test
    public void shouldFailOnNoServiceClusterIpAvailable() {
        Service service = new Service();
        service.setMetadata(new ObjectMeta());
        service.getMetadata().setNamespace(KubernetesSupport.getNamespace(context));
        service.getMetadata().setName("noClusterIpService");
        service.setSpec(new ServiceSpec());

        k8sClient.services().inNamespace(KubernetesSupport.getNamespace(context))
                .resource(service)
                .create();

        Assert.assertThrows(CitrusRuntimeException.class,
                () -> new ServiceClusterIpFunction().execute(List.of("noClusterIpService"), context));
    }

    @Test
    public void shouldResolve() {
        Assert.assertNotNull(new DefaultFunctionLibrary().getFunction("serviceClusterIp"));
    }
}
