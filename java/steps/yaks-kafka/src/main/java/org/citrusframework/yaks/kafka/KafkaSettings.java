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

package org.citrusframework.yaks.kafka;

import org.citrusframework.yaks.YaksSettings;

/**
 * @author Christoph Deppisch
 */
public class KafkaSettings {

    private static final String KAFKA_PROPERTY_PREFIX = "yaks.kafka.";
    private static final String KAFKA_ENV_PREFIX = "YAKS_KAFKA_";

    private static final String CONSUMER_TIMEOUT_PROPERTY = KAFKA_PROPERTY_PREFIX + "timeout";
    private static final String CONSUMER_TIMEOUT_ENV = KAFKA_ENV_PREFIX + "TIMEOUT";
    private static final String CONSUMER_TIMEOUT_DEFAULT = "60000";

    private static final String ENDPOINT_NAME_PROPERTY = KAFKA_PROPERTY_PREFIX + "endpoint.name";
    private static final String ENDPOINT_NAME_ENV = KAFKA_ENV_PREFIX + "ENDPOINT_NAME";
    private static final String ENDPOINT_NAME_DEFAULT = "yaks-kafka-endpoint";

    static final String NAMESPACE_PROPERTY = KAFKA_PROPERTY_PREFIX + "namespace";
    static final String NAMESPACE_ENV = KAFKA_ENV_PREFIX + "NAMESPACE";

    private KafkaSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Timeout when receiving messages.
     * @return time in milliseconds
     */
    public static long getConsumerTimeout() {
        return Long.parseLong(System.getProperty(CONSUMER_TIMEOUT_PROPERTY,
                System.getenv(CONSUMER_TIMEOUT_ENV) != null ? System.getenv(CONSUMER_TIMEOUT_ENV) : CONSUMER_TIMEOUT_DEFAULT));
    }

    /**
     * Default endpoint name to use when creating a Kafka endpoint.
     * @return
     */
    public static String getEndpointName() {
        return System.getProperty(ENDPOINT_NAME_PROPERTY,
                System.getenv(ENDPOINT_NAME_ENV) != null ? System.getenv(ENDPOINT_NAME_ENV) : ENDPOINT_NAME_DEFAULT);
    }

    /**
     * Namespace to work on when performing Kafka client operations such as creating brokers, topics and so on.
     * @return
     */
    public static String getNamespace() {
        return System.getProperty(NAMESPACE_PROPERTY,
                System.getenv(NAMESPACE_ENV) != null ? System.getenv(NAMESPACE_ENV) : YaksSettings.getDefaultNamespace());
    }
}
