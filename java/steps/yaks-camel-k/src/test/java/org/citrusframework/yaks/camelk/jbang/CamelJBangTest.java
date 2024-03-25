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

package org.citrusframework.yaks.camelk.jbang;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import static org.awaitility.Awaitility.await;
import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

public class CamelJBangTest {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CamelJBangTest.class);

    private static Path sampleIntegration;

    @BeforeClass
    public static void setup() throws IOException {
        sampleIntegration = new ClassPathResource("simple.groovy").getFile().toPath();
    }

    @Test
    public void shouldGetIntegrationByPid() {
        Long pid = camel().run("simple", sampleIntegration).getCamelProcessId();

        try {
            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> {
                Map<String, String> integration = camel().get(pid);

                if (integration.isEmpty() || integration.get("STATUS").equals("Starting")) {
                    LOG.info("Waiting for Camel integration to start ...");
                    return false;
                }

                Assert.assertEquals("simple", integration.get("NAME"));

                return true;
            });
        } finally {
            camel().stop(pid);
        }
    }

    @Test
    public void shouldListIntegrations() {
        List<Map<String, String>> integrations = camel().getAll();

        Assert.assertTrue(integrations.stream().noneMatch(row -> "i1".equals(row.get("NAME")) || "i2".equals(row.get("NAME"))));

        Long pid1 = camel().run("i1", sampleIntegration).getCamelProcessId();
        Long pid2 = camel().run("i2", sampleIntegration).getCamelProcessId();

        try {
            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> !camel().get(pid1).isEmpty());
            await().atMost(30000L, TimeUnit.MILLISECONDS).until(() -> !camel().get(pid2).isEmpty());

            integrations = camel().getAll();

            Assert.assertFalse(integrations.isEmpty());
            Assert.assertTrue(integrations.stream().anyMatch(props -> props.get("NAME").equals("i1") && (props.get("STATUS").equals("Starting") || props.get("STATUS").equals("Running"))));
            Assert.assertTrue(integrations.stream().anyMatch(props -> props.get("NAME").equals("i2") && (props.get("STATUS").equals("Starting") || props.get("STATUS").equals("Running"))));
        } finally {
            camel().stop(pid1);
            camel().stop(pid2);
        }
    }

}
