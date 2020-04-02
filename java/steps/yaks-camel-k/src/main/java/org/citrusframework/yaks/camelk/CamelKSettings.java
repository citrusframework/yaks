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

package org.citrusframework.yaks.camelk;

/**
 * @author Christoph Deppisch
 */
public final class CamelKSettings {

    private static final String CAMELK_PROPERTY_PREFIX = "yaks.camelk.";
    private static final String CAMELK_ENV_PREFIX = "YAKS_CAMELK_";

    private static final String MAX_ATTEMPTS_PROPERTY = CAMELK_PROPERTY_PREFIX + "max.attempts";
    private static final String MAX_ATTEMPTS_ENV = CAMELK_ENV_PREFIX + "MAX_ATTEMPTS";
    private static final String MAX_ATTEMPTS_DEFAULT = "150";

    private static final String DELAY_BETWEEN_ATTEMPTS_PROPERTY = CAMELK_PROPERTY_PREFIX + "delay.between.attempts";
    private static final String DELAY_BETWEEN_ATTEMPTS_ENV = CAMELK_ENV_PREFIX + "DELAY_BETWEEN_ATTEMPTS";
    private static final String DELAY_BETWEEN_ATTEMPTS_DEFAULT = "2000";

    private static final String NAMESPACE_PROPERTY = CAMELK_PROPERTY_PREFIX + "namespace";
    private static final String NAMESPACE_ENV = CAMELK_ENV_PREFIX + "NAMESPACE";

    private CamelKSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Maximum number of attempts when polling for running state and log messages.
     * @return
     */
    public static int getMaxAttempts() {
        return Integer.parseInt(System.getProperty(MAX_ATTEMPTS_PROPERTY, System.getenv(MAX_ATTEMPTS_ENV) != null ? System.getenv(MAX_ATTEMPTS_ENV) : MAX_ATTEMPTS_DEFAULT));
    }

    /**
     * Delay in milliseconds to wait after polling attempt.
     * @return
     */
    public static long getDelayBetweenAttempts() {
        return Long.parseLong(System.getProperty(DELAY_BETWEEN_ATTEMPTS_PROPERTY, System.getenv(DELAY_BETWEEN_ATTEMPTS_ENV) != null ? System.getenv(DELAY_BETWEEN_ATTEMPTS_ENV) : DELAY_BETWEEN_ATTEMPTS_DEFAULT));
    }

    /**
     * Namespace to work on when performing Kubernetes client operations such as creating Pods.
     * @return
     */
    public static String getNamespace() {
        return System.getProperty(NAMESPACE_PROPERTY, System.getenv(NAMESPACE_ENV));
    }
}
