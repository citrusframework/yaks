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

package org.citrusframework.yaks.jms.connection.activemq.artemis;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.Map;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.citrusframework.yaks.jms.connection.ConnectionFactoryCreator;

public class ActiveMQArtemisConnectionFactoryCreator implements ConnectionFactoryCreator {

    @Override
    public ConnectionFactory create(Map<String, String> properties) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();

        try {
            if (properties.containsKey("brokerUrl")) {
                connectionFactory.setBrokerURL(properties.get("brokerUrl"));
            }

            if (properties.containsKey("username")) {
                connectionFactory.setUser(properties.get("username"));
            }

            if (properties.containsKey("password")) {
                connectionFactory.setPassword(properties.get("password"));
            }
        } catch (JMSException e) {
            throw new IllegalStateException("Error in initialization ConnectionFactory", e);
        }

        return connectionFactory;
    }

    @Override
    public boolean supports(Class<?> type) {
        return "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory".equals(type.getName());
    }
}
