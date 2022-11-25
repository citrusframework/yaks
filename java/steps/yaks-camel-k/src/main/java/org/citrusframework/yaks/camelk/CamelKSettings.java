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

import java.util.Optional;

import org.citrusframework.yaks.YaksClusterType;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSettings;

/**
 * @author Christoph Deppisch
 */
public final class CamelKSettings {

    private static final String CAMELK_PROPERTY_PREFIX = "yaks.camelk.";
    private static final String CAMELK_ENV_PREFIX = "YAKS_CAMELK_";

    private static final String MAX_ATTEMPTS_PROPERTY = CAMELK_PROPERTY_PREFIX + "max.attempts";
    private static final String MAX_ATTEMPTS_ENV = CAMELK_ENV_PREFIX + "MAX_ATTEMPTS";
    private static final String MAX_ATTEMPTS_DEFAULT = String.valueOf(KubernetesSettings.getMaxAttempts());

    private static final String DELAY_BETWEEN_ATTEMPTS_PROPERTY = CAMELK_PROPERTY_PREFIX + "delay.between.attempts";
    private static final String DELAY_BETWEEN_ATTEMPTS_ENV = CAMELK_ENV_PREFIX + "DELAY_BETWEEN_ATTEMPTS";
    private static final String DELAY_BETWEEN_ATTEMPTS_DEFAULT = String.valueOf(KubernetesSettings.getDelayBetweenAttempts());

    private static final String NAMESPACE_PROPERTY = CAMELK_PROPERTY_PREFIX + "namespace";
    private static final String NAMESPACE_ENV = CAMELK_ENV_PREFIX + "NAMESPACE";

    private static final String OPERATOR_NAMESPACE_PROPERTY = CAMELK_PROPERTY_PREFIX + "operator.namespace";
    private static final String OPERATOR_NAMESPACE_ENV = CAMELK_ENV_PREFIX + "OPERATOR_NAMESPACE";
    private static final String OPERATOR_NAMESPACE_DEFAULT = "camel-system";

    private static final String API_VERSION_PROPERTY = CAMELK_PROPERTY_PREFIX + "api.version";
    private static final String API_VERSION_ENV = CAMELK_ENV_PREFIX + "API_VERSION";
    public static final String API_VERSION_DEFAULT = "v1";

    private static final String KAMELET_API_VERSION_PROPERTY = CAMELK_PROPERTY_PREFIX + "kamelet.api.version";
    private static final String KAMELET_API_VERSION_ENV = CAMELK_ENV_PREFIX + "_KAMELET_API_VERSION";
    public static final String KAMELET_API_VERSION_DEFAULT = "v1alpha1";

    private static final String AUTO_REMOVE_RESOURCES_PROPERTY = CAMELK_PROPERTY_PREFIX + "auto.remove.resources";
    private static final String AUTO_REMOVE_RESOURCES_ENV = CAMELK_ENV_PREFIX + "AUTO_REMOVE_RESOURCES";
    private static final String AUTO_REMOVE_RESOURCES_DEFAULT = "true";

    private static final String SUPPORT_VARIABLES_IN_SOURCES_PROPERTY = CAMELK_PROPERTY_PREFIX + "support.variables.in.sources";
    private static final String SUPPORT_VARIABLES_IN_SOURCES_ENV = CAMELK_ENV_PREFIX + "SUPPORT_VARIABLES_IN_SOURCES";
    private static final String SUPPORT_VARIABLES_IN_SOURCES_DEFAULT = "true";

    private static final String PRINT_POD_LOGS_PROPERTY = CAMELK_PROPERTY_PREFIX + "print.pod.logs";
    private static final String PRINT_POD_LOGS_ENV = CAMELK_ENV_PREFIX + "PRINT_POD_LOGS";
    private static final String PRINT_POD_LOGS_DEFAULT = String.valueOf(KubernetesSettings.isPrintPodLogs());

    public static final String INTEGRATION_LABEL = "camel.apache.org/integration";

    private CamelKSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Api version for current Camel K installation.
     * @return
     */
    public static String getApiVersion() {
        return System.getProperty(API_VERSION_PROPERTY,
                System.getenv(API_VERSION_ENV) != null ? System.getenv(API_VERSION_ENV) : API_VERSION_DEFAULT);
    }

    /**
     * Api version for current Kamelet specification.
     * @return
     */
    public static String getKameletApiVersion() {
        return System.getProperty(KAMELET_API_VERSION_PROPERTY,
                System.getenv(KAMELET_API_VERSION_ENV) != null ? System.getenv(KAMELET_API_VERSION_ENV) : KAMELET_API_VERSION_DEFAULT);
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

    /**
     * Namespace to work on when performing Kubernetes client operations such as creating Pods.
     * @return
     */
    public static String getNamespace() {
        return System.getProperty(NAMESPACE_PROPERTY,
                System.getenv(NAMESPACE_ENV) != null ? System.getenv(NAMESPACE_ENV) : YaksSettings.getDefaultNamespace());
    }

    /**
     * Camel K operator namespace.
     * @return
     */
    public static String getOperatorNamespace() {
        return Optional.ofNullable(System.getProperty(OPERATOR_NAMESPACE_PROPERTY, System.getenv(OPERATOR_NAMESPACE_ENV)))
                .orElseGet(() -> YaksSettings.getClusterType().equals(YaksClusterType.KUBERNETES) ?
                        OPERATOR_NAMESPACE_DEFAULT : YaksSettings.getOperatorNamespace());

    }

    /**
     * When set to true Camel K resources (integrations, Kamelets etc.) created during the test are
     * automatically removed after the test.
     * @return
     */
    public static boolean isAutoRemoveResources() {
        return Boolean.parseBoolean(System.getProperty(AUTO_REMOVE_RESOURCES_PROPERTY,
                System.getenv(AUTO_REMOVE_RESOURCES_ENV) != null ? System.getenv(AUTO_REMOVE_RESOURCES_ENV) : AUTO_REMOVE_RESOURCES_DEFAULT));
    }

    /**
     * When set to true YAKS will replace test variables in Camel K sources.
     * In certain circumstances this may raise unknown variable errors when Camel body expressions are used (${body}).
     * @return
     */
    public static boolean isSupportVariablesInSources() {
        return Boolean.parseBoolean(System.getProperty(SUPPORT_VARIABLES_IN_SOURCES_PROPERTY,
                System.getenv(SUPPORT_VARIABLES_IN_SOURCES_ENV) != null ? System.getenv(SUPPORT_VARIABLES_IN_SOURCES_ENV) : SUPPORT_VARIABLES_IN_SOURCES_DEFAULT));
    }

    /**
     * When set to true test will print pod logs e.g. while waiting for a pod log message.
     * @return
     */
    public static boolean isPrintPodLogs() {
        return Boolean.parseBoolean(System.getProperty(PRINT_POD_LOGS_PROPERTY,
                System.getenv(PRINT_POD_LOGS_ENV) != null ? System.getenv(PRINT_POD_LOGS_ENV) : PRINT_POD_LOGS_DEFAULT));
    }
}
