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

import java.util.Optional;

import org.citrusframework.yaks.YaksSettings;

/**
 * @author Christoph Deppisch
 */
public class TestContainersSettings {

    static final String TESTCONTAINERS_PROPERTY_PREFIX = "yaks.testcontainers.";
    static final String TESTCONTAINERS_ENV_PREFIX = "YAKS_TESTCONTAINERS_";

    private static final String AUTO_REMOVE_RESOURCES_PROPERTY = TESTCONTAINERS_PROPERTY_PREFIX + "auto.remove.resources";
    private static final String AUTO_REMOVE_RESOURCES_ENV = TESTCONTAINERS_ENV_PREFIX + "AUTO_REMOVE_RESOURCES";
    private static final String AUTO_REMOVE_RESOURCES_DEFAULT = "true";

    private static final String KUBEDOCK_ENABLED_PROPERTY = TESTCONTAINERS_PROPERTY_PREFIX + "kubedock.enabled";
    private static final String KUBEDOCK_ENABLED_ENV = TESTCONTAINERS_ENV_PREFIX + "KUBEDOCK_ENABLED";

    private static final String TEST_ID_PROPERTY = "yaks.test.id";
    private static final String TEST_ID_ENV = "YAKS_TEST_ID";
    private static final String TEST_ID_DEFAULT = "yaks-test";

    private static final String TEST_NAME_PROPERTY = "yaks.test.name";
    private static final String TEST_NAME_ENV = "YAKS_TEST_NAME";
    private static final String TEST_NAME_DEFAULT = "yaks";

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
     * True when using KubeDock services.
     * @return
     */
    public static boolean isKubedockEnabled() {
        return Boolean.parseBoolean(System.getProperty(KUBEDOCK_ENABLED_PROPERTY,
                Optional.ofNullable(System.getenv(KUBEDOCK_ENABLED_ENV)).orElseGet(() -> String.valueOf(!YaksSettings.isLocal()))));
    }

    /**
     * Current test id that is also set as label on the Pod running the test.
     * @return
     */
    public static String getTestId() {
        return System.getProperty(TEST_ID_PROPERTY, Optional.ofNullable(System.getenv(TEST_ID_ENV)).orElse(TEST_ID_DEFAULT));
    }

    /**
     * Current test id that is also set as label on the Pod running the test.
     * @return
     */
    public static String getTestName() {
        return System.getProperty(TEST_NAME_PROPERTY, Optional.ofNullable(System.getenv(TEST_NAME_ENV)).orElse(TEST_NAME_DEFAULT));
    }
}
