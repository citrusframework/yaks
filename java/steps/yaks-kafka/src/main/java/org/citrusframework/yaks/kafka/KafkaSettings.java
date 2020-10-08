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

package org.citrusframework.yaks.kafka;

/**
 * @author Christoph Deppisch
 */
public class KafkaSettings {

    private static final String KNATIVE_PROPERTY_PREFIX = "yaks.kafka.";
    private static final String KNATIVE_ENV_PREFIX = "YAKS_KAFKA_";

    private static final String CONSUMER_TIMEOUT_PROPERTY = KNATIVE_PROPERTY_PREFIX + "timeout";
    private static final String CONSUMER_TIMEOUT_ENV = KNATIVE_ENV_PREFIX + "TIMEOUT";
    private static final String CONSUMER_TIMEOUT_DEFAULT = "60000";

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
}
