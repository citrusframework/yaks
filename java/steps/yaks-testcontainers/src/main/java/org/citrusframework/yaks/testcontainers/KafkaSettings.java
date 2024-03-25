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

package org.citrusframework.yaks.testcontainers;

/**
 * @author Christoph Deppisch
 */
public class KafkaSettings {

    private static final String KAFKA_PROPERTY_PREFIX = TestContainersSettings.TESTCONTAINERS_PROPERTY_PREFIX + "kafka.";
    private static final String KAFKA_ENV_PREFIX = TestContainersSettings.TESTCONTAINERS_ENV_PREFIX + "KAFKA_";

    private static final String KAFKA_VERSION_PROPERTY = KAFKA_PROPERTY_PREFIX + "version";
    private static final String KAFKA_VERSION_ENV = KAFKA_ENV_PREFIX + "KAFKA_VERSION";
    private static final String KAFKA_VERSION_DEFAULT = "7.5.1";

    private static final String KAFKA_SERVICE_NAME_PROPERTY = KAFKA_PROPERTY_PREFIX + "service.name";
    private static final String KAFKA_SERVICE_NAME_ENV = KAFKA_ENV_PREFIX + "KAFKA_SERVICE_NAME";
    private static final String KAFKA_SERVICE_NAME_DEFAULT = "yaks-kafka";

    private static final String KAFKA_IMAGE_NAME_PROPERTY = KAFKA_PROPERTY_PREFIX + "image.name";
    private static final String KAFKA_IMAGE_NAME_ENV = KAFKA_ENV_PREFIX + "KAFKA_IMAGE_NAME";
    private static final String KAFKA_IMAGE_NAME_DEFAULT = "confluentinc/cp-kafka";

    private static final String STARTUP_TIMEOUT_PROPERTY = KAFKA_PROPERTY_PREFIX + "startup.timeout";
    private static final String STARTUP_TIMEOUT_ENV = KAFKA_ENV_PREFIX + "STARTUP_TIMEOUT";
    private static final String STARTUP_TIMEOUT_DEFAULT = "180";

    private KafkaSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Kafka version setting.
     * @return
     */
    public static String getImageName() {
        return System.getProperty(KAFKA_IMAGE_NAME_PROPERTY,
                System.getenv(KAFKA_IMAGE_NAME_ENV) != null ? System.getenv(KAFKA_IMAGE_NAME_ENV) : KAFKA_IMAGE_NAME_DEFAULT);
    }

    /**
     * Kafka version setting.
     * @return
     */
    public static String getVersion() {
        return System.getProperty(KAFKA_VERSION_PROPERTY,
                System.getenv(KAFKA_VERSION_ENV) != null ? System.getenv(KAFKA_VERSION_ENV) : KAFKA_VERSION_DEFAULT);
    }

    /**
     * Kafka service name.
     * @return
     */
    public static String getServiceName() {
        return System.getProperty(KAFKA_SERVICE_NAME_PROPERTY,
                System.getenv(KAFKA_SERVICE_NAME_ENV) != null ? System.getenv(KAFKA_SERVICE_NAME_ENV) : KAFKA_SERVICE_NAME_DEFAULT);
    }

    /**
     * Time in seconds to wait for the container to startup and accept connections.
     * @return
     */
    public static int getStartupTimeout() {
        return Integer.parseInt(System.getProperty(STARTUP_TIMEOUT_PROPERTY,
                System.getenv(STARTUP_TIMEOUT_ENV) != null ? System.getenv(STARTUP_TIMEOUT_ENV) : STARTUP_TIMEOUT_DEFAULT));
    }
}
