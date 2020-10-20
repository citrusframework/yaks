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

package org.citrusframework.yaks.kubernetes;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.citrusframework.yaks.YaksSettings;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 */
public class KubernetesSettings {

    private static final String KUBERNETES_PROPERTY_PREFIX = "yaks.kubernetes.";
    private static final String KUBERNETES_ENV_PREFIX = "YAKS_KUBERNETES_";

    private static final String SERVICE_TIMEOUT_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "event.consumer.timeout";
    private static final String SERVICE_TIMEOUT_ENV = KUBERNETES_ENV_PREFIX + "SERVICE_TIMEOUT";
    private static final String SERVICE_TIMEOUT_DEFAULT = "2000";

    private static final String NAMESPACE_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "namespace";
    private static final String NAMESPACE_ENV = KUBERNETES_ENV_PREFIX + "NAMESPACE";

    private static final String API_VERSION_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "api.version";
    private static final String API_VERSION_ENV = KUBERNETES_ENV_PREFIX + "API_VERSION";
    private static final String API_VERSION_DEFAULT = "v1";

    private static final String SERVICE_NAME_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "service.name";
    private static final String SERVICE_NAME_ENV = KUBERNETES_ENV_PREFIX + "SERVICE_NAME";
    private static final String SERVICE_NAME_DEFAULT = "yaks-k8s-service";

    private static final String SERVICE_PORT_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "service.port";
    private static final String SERVICE_PORT_ENV = KUBERNETES_ENV_PREFIX + "SERVICE_PORT";
    private static final String SERVICE_PORT_DEFAULT = "8080";

    private static final String AUTO_REMOVE_RESOURCES_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "auto.remove.resources";
    private static final String AUTO_REMOVE_RESOURCES_ENV = KUBERNETES_ENV_PREFIX + "AUTO_REMOVE_RESOURCES";
    private static final String AUTO_REMOVE_RESOURCES_DEFAULT = "true";

    private static final String DEFAULT_LABELS_PROPERTY = KUBERNETES_PROPERTY_PREFIX + "default.labels";
    private static final String DEFAULT_LABELS_ENV = KUBERNETES_ENV_PREFIX + "DEFAULT_LABELS";
    private static final String DEFAULT_LABELS_DEFAULT = "app=yaks";

    private KubernetesSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Request timeout when receiving cloud events.
     * @return
     */
    public static long getServiceTimeout() {
        return Long.parseLong(System.getProperty(SERVICE_TIMEOUT_PROPERTY,
                System.getenv(SERVICE_TIMEOUT_ENV) != null ? System.getenv(SERVICE_TIMEOUT_ENV) : SERVICE_TIMEOUT_DEFAULT));
    }

    /**
     * Namespace to work on when performing Kubernetes client operations such as creating triggers, services and so on.
     * @return
     */
    public static String getNamespace() {
        return System.getProperty(NAMESPACE_PROPERTY,
                System.getenv(NAMESPACE_ENV) != null ? System.getenv(NAMESPACE_ENV) : YaksSettings.getDefaultNamespace());
    }

    /**
     * Api version for current Kubernetes installation.
     * @return
     */
    public static String getApiVersion() {
        return System.getProperty(API_VERSION_PROPERTY,
                System.getenv(API_VERSION_ENV) != null ? System.getenv(API_VERSION_ENV) : API_VERSION_DEFAULT);
    }

    /**
     * Service name to use when creating a new service for cloud event subscriptions.
     * @return
     */
    public static String getServiceName() {
        return System.getProperty(SERVICE_NAME_PROPERTY,
                System.getenv(SERVICE_NAME_ENV) != null ? System.getenv(SERVICE_NAME_ENV) : SERVICE_NAME_DEFAULT);
    }

    /**
     * Service port used when consuming cloud events via Http.
     * @return
     */
    public static int getServicePort() {
        return Integer.parseInt(System.getProperty(SERVICE_PORT_PROPERTY,
                System.getenv(SERVICE_PORT_ENV) != null ? System.getenv(SERVICE_PORT_ENV) : SERVICE_PORT_DEFAULT));
    }

    /**
     * Read labels for Kubernetes resources created by the test. The environment setting should be a
     * comma delimited list of key-value pairs.
     * @return
     */
    public static Map<String, String> getDefaultLabels() {
        String labelsConfig = System.getProperty(DEFAULT_LABELS_PROPERTY,
                System.getenv(DEFAULT_LABELS_ENV) != null ? System.getenv(DEFAULT_LABELS_ENV) : DEFAULT_LABELS_DEFAULT);

        return Stream.of(StringUtils.commaDelimitedListToStringArray(labelsConfig))
                    .map(item -> StringUtils.delimitedListToStringArray(item, "="))
                    .filter(keyValue -> keyValue.length == 2)
                    .collect(Collectors.toMap(item -> item[0], item -> item[1]));
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
}
