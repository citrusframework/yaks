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

package org.citrusframework.yaks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Deppisch
 */
public class YaksSettings {

    /** Logger */
    private static Logger LOG = LoggerFactory.getLogger(YaksSettings.class);

    private static final String YAKS_PROPERTY_PREFIX = "yaks.";
    private static final String YAKS_ENV_PREFIX = "YAKS_";

    private static final String CLUSTER_WILDCARD_DOMAIN_PROPERTY = "cluster.wildcard.domain";
    private static final String CLUSTER_WILDCARD_DOMAIN_ENV = "CLUSTER_WILDCARD_DOMAIN";
    public static final String DEFAULT_DOMAIN_SUFFIX = "svc.cluster.local";

    private static final String NAMESPACE_PROPERTY = YAKS_PROPERTY_PREFIX + "namespace";
    private static final String NAMESPACE_ENV = YAKS_ENV_PREFIX + "NAMESPACE";
    private static final String NAMESPACE_DEFAULT = "default";

    /**
     * Namespace to work on when performing Kubernetes/Knative client operations on resources.
     * @return
     */
    public static String getDefaultNamespace() {
        String systemNamespace = System.getProperty(NAMESPACE_PROPERTY, System.getenv(NAMESPACE_ENV));

        if (systemNamespace != null) {
            return systemNamespace;
        }

        final File namespace = new File("/var/run/secrets/kubernetes.io/serviceaccount/namespace");
        if (namespace.exists()){
            try {
                return new String(Files.readAllBytes(namespace.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.warn("Failed to read Kubernetes namespace from filesystem {}", namespace, e);
            }
        }

        return NAMESPACE_DEFAULT;
    }

    /**
     * Cluster wildcard domain or default if non is set.
     * @return
     */
    public static String getClusterWildcardDomain() {
        return System.getProperty(CLUSTER_WILDCARD_DOMAIN_PROPERTY,
                System.getenv(CLUSTER_WILDCARD_DOMAIN_ENV) != null ? System.getenv(CLUSTER_WILDCARD_DOMAIN_ENV) : "default." + DEFAULT_DOMAIN_SUFFIX);
    }
}
