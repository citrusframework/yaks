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
package org.citrusframework.yaks.jms;

import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JmsBrokerConfiguration {

	@Bean(initMethod = "start", destroyMethod = "stop")
	public BrokerService messageBroker() {
		try {
			BrokerService messageBroker = BrokerFactory.createBroker("broker:tcp://localhost:61616");
			messageBroker.setPersistent(false);
			messageBroker.setUseJmx(false);
			return messageBroker;
		} catch (Exception e) {
			throw new BeanCreationException("Failed to create embedded message broker", e);
		}
	}
}
