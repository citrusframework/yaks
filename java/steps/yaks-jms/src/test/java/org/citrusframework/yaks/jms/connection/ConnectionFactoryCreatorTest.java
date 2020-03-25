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

package org.citrusframework.yaks.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.assertj.core.api.Assertions;
import org.citrusframework.yaks.jms.connection.activemq.ActiveMQConnectionFactoryCreator;
import org.junit.Test;

/**
 * @author Christoph Deppisch
 */
public class ConnectionFactoryCreatorTest {

    @Test
    public void shouldLookup() throws ClassNotFoundException {
        ConnectionFactoryCreator creator = ConnectionFactoryCreator.lookup(ActiveMQConnectionFactory.class.getName());
        Assertions.assertThat(ActiveMQConnectionFactoryCreator.class).isEqualTo(creator.getClass());
    }

    @Test
    public void shouldLookupDefault() throws ClassNotFoundException {
        ConnectionFactoryCreator creator = ConnectionFactoryCreator.lookup(DummyConnectionFactory.class.getName());
        Assertions.assertThat(DefaultConnectionFactoryCreator.class).isEqualTo(creator.getClass());
    }

    @Test(expected = ClassNotFoundException.class)
    public void shouldHandleUnsupportedTypeInformation() throws ClassNotFoundException {
        ConnectionFactoryCreator.lookup("org.unknown.Type");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldHandleMissingTypeInformation() throws ClassNotFoundException {
        ConnectionFactoryCreator.lookup(null);
    }

    /**
     * Empty connection factory implementation for testing.
     */
    private static class DummyConnectionFactory implements ConnectionFactory {
        @Override
        public Connection createConnection() throws JMSException {
            return null;
        }

        @Override
        public Connection createConnection(String s, String s1) throws JMSException {
            return null;
        }

        @Override
        public JMSContext createContext() {
            return null;
        }

        @Override
        public JMSContext createContext(String s, String s1) {
            return null;
        }

        @Override
        public JMSContext createContext(String s, String s1, int i) {
            return null;
        }

        @Override
        public JMSContext createContext(int i) {
            return null;
        }
    }
}
