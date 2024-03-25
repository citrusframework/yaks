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
public class MongoDBSettings {

    private static final String MONGODB_PROPERTY_PREFIX = TestContainersSettings.TESTCONTAINERS_PROPERTY_PREFIX + "mongodb.";
    private static final String MONGODB_ENV_PREFIX = TestContainersSettings.TESTCONTAINERS_ENV_PREFIX + "MONGODB_";

    private static final String VERSION_PROPERTY = MONGODB_PROPERTY_PREFIX + "version";
    private static final String VERSION_ENV = MONGODB_ENV_PREFIX + "VERSION";
    private static final String VERSION_DEFAULT = "4.0.10";

    private static final String SERVICE_NAME_PROPERTY = MONGODB_PROPERTY_PREFIX + "service.name";
    private static final String SERVICE_NAME_ENV = MONGODB_ENV_PREFIX + "SERVICE_NAME";
    private static final String SERVICE_NAME_DEFAULT = "yaks-mongodb";

    private static final String STARTUP_TIMEOUT_PROPERTY = MONGODB_PROPERTY_PREFIX + "startup.timeout";
    private static final String STARTUP_TIMEOUT_ENV = MONGODB_ENV_PREFIX + "STARTUP_TIMEOUT";
    private static final String STARTUP_TIMEOUT_DEFAULT = "180";

    private MongoDBSettings() {
        // prevent instantiation of utility class
    }

    /**
     * MongoDB version setting.
     * @return
     */
    public static String getMongoDBVersion() {
        return System.getProperty(VERSION_PROPERTY,
                System.getenv(VERSION_ENV) != null ? System.getenv(VERSION_ENV) : VERSION_DEFAULT);
    }

    /**
     * MongoDB service name.
     * @return
     */
    public static String getServiceName() {
        return System.getProperty(SERVICE_NAME_PROPERTY,
                System.getenv(SERVICE_NAME_ENV) != null ? System.getenv(SERVICE_NAME_ENV) : SERVICE_NAME_DEFAULT);
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
