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
public class RedpandaSettings {

    private static final String REDPANDA_PROPERTY_PREFIX = TestContainersSettings.TESTCONTAINERS_PROPERTY_PREFIX + "redpanda.";
    private static final String REDPANDA_ENV_PREFIX = TestContainersSettings.TESTCONTAINERS_ENV_PREFIX + "REDPANDA_";

    private static final String REDPANDA_VERSION_PROPERTY = REDPANDA_PROPERTY_PREFIX + "version";
    private static final String REDPANDA_VERSION_ENV = REDPANDA_ENV_PREFIX + "REDPANDA_VERSION";
    private static final String REDPANDA_VERSION_DEFAULT = "v23.1.19";

    private static final String REDPANDA_SERVICE_NAME_PROPERTY = REDPANDA_PROPERTY_PREFIX + "service.name";
    private static final String REDPANDA_SERVICE_NAME_ENV = REDPANDA_ENV_PREFIX + "REDPANDA_SERVICE_NAME";
    private static final String REDPANDA_SERVICE_NAME_DEFAULT = "yaks-redpanda";

    private static final String REDPANDA_IMAGE_NAME_PROPERTY = REDPANDA_PROPERTY_PREFIX + "image.name";
    private static final String REDPANDA_IMAGE_NAME_ENV = REDPANDA_ENV_PREFIX + "REDPANDA_IMAGE_NAME";
    private static final String REDPANDA_IMAGE_NAME_DEFAULT = "docker.redpanda.com/vectorized/redpanda";

    private static final String STARTUP_TIMEOUT_PROPERTY = REDPANDA_PROPERTY_PREFIX + "startup.timeout";
    private static final String STARTUP_TIMEOUT_ENV = REDPANDA_ENV_PREFIX + "STARTUP_TIMEOUT";
    private static final String STARTUP_TIMEOUT_DEFAULT = "180";

    private RedpandaSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Redpanda version setting.
     * @return
     */
    public static String getImageName() {
        return System.getProperty(REDPANDA_IMAGE_NAME_PROPERTY,
                System.getenv(REDPANDA_IMAGE_NAME_ENV) != null ? System.getenv(REDPANDA_IMAGE_NAME_ENV) : REDPANDA_IMAGE_NAME_DEFAULT);
    }

    /**
     * Redpanda version setting.
     * @return
     */
    public static String getVersion() {
        return System.getProperty(REDPANDA_VERSION_PROPERTY,
                System.getenv(REDPANDA_VERSION_ENV) != null ? System.getenv(REDPANDA_VERSION_ENV) : REDPANDA_VERSION_DEFAULT);
    }

    /**
     * Redpanda service name setting.
     * @return
     */
    public static String getServiceName() {
        return System.getProperty(REDPANDA_SERVICE_NAME_PROPERTY,
                System.getenv(REDPANDA_SERVICE_NAME_ENV) != null ? System.getenv(REDPANDA_SERVICE_NAME_ENV) : REDPANDA_SERVICE_NAME_DEFAULT);
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
