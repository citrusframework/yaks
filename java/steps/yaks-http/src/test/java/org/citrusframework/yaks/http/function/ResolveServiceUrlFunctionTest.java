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

package org.citrusframework.yaks.http.function;

import java.util.Arrays;
import java.util.Collections;

import org.citrusframework.context.TestContext;
import org.citrusframework.context.TestContextFactory;
import org.citrusframework.functions.DefaultFunctionLibrary;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.yaks.YaksClusterType;
import org.junit.Assert;
import org.junit.Test;

public class ResolveServiceUrlFunctionTest {

    private final ResolveServiceUrlFunction function = new ResolveServiceUrlFunction();

    @Test
    public void shouldResolveService() {
        TestContext context = TestContextFactory.newInstance().getObject();

        context.setVariable("YAKS_NAMESPACE", "default");

        Assert.assertEquals("http://test-service.default", function.execute(Collections.singletonList("test-service"), context));
        Assert.assertEquals("http://test-service.default", function.execute(Arrays.asList("test-service", "8080"), context));
    }

    @Test
    public void shouldResolveSecureService() {
        TestContext context = TestContextFactory.newInstance().getObject();

        context.setVariable("YAKS_NAMESPACE", "default");

        Assert.assertEquals("https://test-service.default", function.execute(Arrays.asList("test-service", "TRUE"), context));
        Assert.assertEquals("https://test-service.default", function.execute(Arrays.asList("test-service", "8080", "TRUE"), context));

    }

    @Test
    public void shouldResolveLocalService() {
        try {
            System.setProperty("yaks.cluster.type", YaksClusterType.LOCAL.name());
            TestContext context = TestContextFactory.newInstance().getObject();

            context.getReferenceResolver().bind("test-service", new HttpServerBuilder()
                    .autoStart(false)
                    .port(8888)
                    .build());

            Assert.assertEquals("http://localhost:8888", function.execute(Collections.singletonList("test-service"), context));
            Assert.assertEquals("http://localhost", function.execute(Collections.singletonList("foo-service"), context));
            Assert.assertEquals("http://localhost:8080", function.execute(Arrays.asList("foo-service", "8080"), context));
        } finally {
            System.setProperty("yaks.cluster.type", YaksClusterType.KUBERNETES.name());
        }
    }

    @Test
    public void shouldResolve() {
        Assert.assertNotNull(new DefaultFunctionLibrary().getFunction("resolveURL"));
    }
}
