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

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Christoph Deppisch
 */
public class JmsSettings {

    private static final String JMS_PROPERTY_PREFIX = "yaks.kubernetes.";
    private static final String JMS_ENV_PREFIX = "YAKS_JMS_";

    private static final String ENDPOINT_NAME_PROPERTY = JMS_PROPERTY_PREFIX + "endpoint.name";
    private static final String ENDPOINT_NAME_ENV = JMS_ENV_PREFIX + "ENDPOINT_NAME";
    private static final String ENDPOINT_NAME_DEFAULT = "yaks-jms-endpoint";

    private static final String TIMEOUT_PROPERTY = JMS_PROPERTY_PREFIX + "timeout";
    private static final String TIMEOUT_ENV = JMS_ENV_PREFIX + "TIMEOUT";

    private JmsSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Request timeout when receiving messages.
     * @return
     */
    public static long getTimeout() {
        return Optional.ofNullable(System.getProperty(TIMEOUT_PROPERTY, System.getenv(TIMEOUT_ENV)))
                .map(Long::parseLong)
                .orElse(TimeUnit.SECONDS.toMillis(60));
    }

    /**
     * Default endpoint name to use when creating a Kafka endpoint.
     * @return
     */
    public static String getEndpointName() {
        return System.getProperty(ENDPOINT_NAME_PROPERTY,
                System.getenv(ENDPOINT_NAME_ENV) != null ? System.getenv(ENDPOINT_NAME_ENV) : ENDPOINT_NAME_DEFAULT);
    }
}
