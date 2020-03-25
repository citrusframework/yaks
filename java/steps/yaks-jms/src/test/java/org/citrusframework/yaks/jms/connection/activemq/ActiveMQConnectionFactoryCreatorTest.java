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

package org.citrusframework.yaks.jms.connection.activemq;

import javax.jms.ConnectionFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class ActiveMQConnectionFactoryCreatorTest {

    private ActiveMQConnectionFactoryCreator connectionFactoryCreator = new ActiveMQConnectionFactoryCreator();

    @Test
    public void shouldCreate() {
        ConnectionFactory connectionFactory = connectionFactoryCreator.create(Collections.emptyMap());

        Assertions.assertThat(ActiveMQConnectionFactory.class).isEqualTo(connectionFactory.getClass());
        Assertions.assertThat(ActiveMQConnectionFactory.DEFAULT_BROKER_URL).isEqualTo(((ActiveMQConnectionFactory)connectionFactory).getBrokerURL());
    }

    @Test
    public void shouldCreateWithProperties() {
        Map<String, String> connectionSettings = new LinkedHashMap<>();
        connectionSettings.put("brokerUrl", "typ://localhost:61617");
        connectionSettings.put("username", "foo");
        connectionSettings.put("password", "secret");
        ConnectionFactory connectionFactory = connectionFactoryCreator.create(connectionSettings);

        Assertions.assertThat(ActiveMQConnectionFactory.class).isEqualTo(connectionFactory.getClass());
        Assertions.assertThat("typ://localhost:61617").isEqualTo(((ActiveMQConnectionFactory)connectionFactory).getBrokerURL());
        Assertions.assertThat("foo").isEqualTo(((ActiveMQConnectionFactory)connectionFactory).getUserName());
        Assertions.assertThat("secret").isEqualTo(((ActiveMQConnectionFactory)connectionFactory).getPassword());
    }

    @Test
    public void shouldSupport() {
        Assertions.assertThat(connectionFactoryCreator.supports(ActiveMQConnectionFactory.class)).isTrue();
        Assertions.assertThat(connectionFactoryCreator.supports(ConnectionFactory.class)).isFalse();
    }
}
