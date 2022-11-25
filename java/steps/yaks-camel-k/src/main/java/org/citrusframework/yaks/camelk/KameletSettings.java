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
public final class KameletSettings {

    private static final String KAMELET_PROPERTY_PREFIX = "yaks.kamelet.";
    private static final String KAMELET_ENV_PREFIX = "YAKS_KAMELET_";

    private static final String NAMESPACE_PROPERTY = KAMELET_PROPERTY_PREFIX + "namespace";
    private static final String NAMESPACE_ENV = KAMELET_ENV_PREFIX + "NAMESPACE";

    private KameletSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Default namespace for Kamelets.
     * @return
     */
    public static String getNamespace() {
        return System.getProperty(NAMESPACE_PROPERTY,
                System.getenv(NAMESPACE_ENV) != null ? System.getenv(NAMESPACE_ENV) : CamelKSettings.getNamespace());
    }

}
