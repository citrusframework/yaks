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

package org.citrusframework.yaks.testcontainers;

/**
 * @author Christoph Deppisch
 */
public class TestContainersSettings {

    static final String TESTCONTAINERS_PROPERTY_PREFIX = "yaks.testcontainers.";
    static final String TESTCONTAINERS_ENV_PREFIX = "YAKS_TESTCONTAINERS_";

    private static final String AUTO_REMOVE_RESOURCES_PROPERTY = TESTCONTAINERS_PROPERTY_PREFIX + "auto.remove.resources";
    private static final String AUTO_REMOVE_RESOURCES_ENV = TESTCONTAINERS_ENV_PREFIX + "AUTO_REMOVE_RESOURCES";
    private static final String AUTO_REMOVE_RESOURCES_DEFAULT = "true";

    private static final String MAX_ATTEMPTS_PROPERTY = TESTCONTAINERS_PROPERTY_PREFIX + "max.attempts";
    private static final String MAX_ATTEMPTS_ENV = TESTCONTAINERS_ENV_PREFIX + "MAX_ATTEMPTS";
    private static final String MAX_ATTEMPTS_DEFAULT = "150";

    private static final String DELAY_BETWEEN_ATTEMPTS_PROPERTY = TESTCONTAINERS_PROPERTY_PREFIX + "delay.between.attempts";
    private static final String DELAY_BETWEEN_ATTEMPTS_ENV = TESTCONTAINERS_ENV_PREFIX + "DELAY_BETWEEN_ATTEMPTS";
    private static final String DELAY_BETWEEN_ATTEMPTS_DEFAULT = "2000";

    private TestContainersSettings() {
        // prevent instantiation of utility class
    }

/**
     * When set to true Kubernetes resources (e.g. services) created during the test are
     * automatically removed after the test.
     * @return
     */
    public static boolean isAutoRemoveResources() {
        return Boolean.parseBoolean(System.getProperty(AUTO_REMOVE_RESOURCES_PROPERTY,
                System.getenv(AUTO_REMOVE_RESOURCES_ENV) != null ? System.getenv(AUTO_REMOVE_RESOURCES_ENV) : AUTO_REMOVE_RESOURCES_DEFAULT));
    }

    /**
     * Maximum number of attempts when polling for running state and log messages.
     * @return
     */
    public static int getMaxAttempts() {
        return Integer.parseInt(System.getProperty(MAX_ATTEMPTS_PROPERTY,
                System.getenv(MAX_ATTEMPTS_ENV) != null ? System.getenv(MAX_ATTEMPTS_ENV) : MAX_ATTEMPTS_DEFAULT));
    }

    /**
     * Delay in milliseconds to wait after polling attempt.
     * @return
     */
    public static long getDelayBetweenAttempts() {
        return Long.parseLong(System.getProperty(DELAY_BETWEEN_ATTEMPTS_PROPERTY,
                System.getenv(DELAY_BETWEEN_ATTEMPTS_ENV) != null ? System.getenv(DELAY_BETWEEN_ATTEMPTS_ENV) : DELAY_BETWEEN_ATTEMPTS_DEFAULT));
    }
}
