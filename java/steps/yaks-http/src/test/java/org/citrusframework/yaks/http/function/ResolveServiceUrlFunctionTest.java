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

    @Test
    public void shouldResolveService() {
        ResolveServiceUrlFunction function = new ResolveServiceUrlFunction();
        TestContext context = TestContextFactory.newInstance().getObject();

        context.setVariable("YAKS_NAMESPACE", "default");

        Assert.assertEquals(function.execute(Collections.singletonList("test-service"), context), "http://test-service.default");
    }

    @Test
    public void shouldResolveSecureService() {
        ResolveServiceUrlFunction function = new ResolveServiceUrlFunction();
        TestContext context = TestContextFactory.newInstance().getObject();

        context.setVariable("YAKS_NAMESPACE", "default");

        Assert.assertEquals(function.execute(Arrays.asList("test-service", "TRUE"), context), "https://test-service.default");

    }

    @Test
    public void shouldResolveLocalService() {
        try {
            System.setProperty("yaks.cluster.type", YaksClusterType.LOCAL.name());
            ResolveServiceUrlFunction function = new ResolveServiceUrlFunction();
            TestContext context = TestContextFactory.newInstance().getObject();

            context.getReferenceResolver().bind("test-service", new HttpServerBuilder()
                    .autoStart(false)
                    .port(8888)
                    .build());

            Assert.assertEquals(function.execute(Collections.singletonList("test-service"), context), "http://localhost:8888");
        } finally {
            System.setProperty("yaks.cluster.type", YaksClusterType.KUBERNETES.name());
        }
    }

    @Test
    public void shouldResolve() {
        Assert.assertNotNull(new DefaultFunctionLibrary().getFunction("resolveURL"));
    }
}
