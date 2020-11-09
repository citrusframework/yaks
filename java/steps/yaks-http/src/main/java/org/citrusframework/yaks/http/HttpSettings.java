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

package org.citrusframework.yaks.http;

/**
 * @author Christoph Deppisch
 */
public class HttpSettings {

    private static final String HTTP_PROPERTY_PREFIX = "yaks.kubernetes.";
    private static final String HTTP_ENV_PREFIX = "YAKS_HTTP_";

    private static final String TIMEOUT_PROPERTY = HTTP_PROPERTY_PREFIX + ".timeout";
    private static final String TIMEOUT_ENV = HTTP_ENV_PREFIX + "_TIMEOUT";
    private static final String TIMEOUT_DEFAULT = "2000";

    private static final String FORK_MODE_PROPERTY = HTTP_PROPERTY_PREFIX + ".fork.mode";
    private static final String FORK_MODE_ENV = HTTP_ENV_PREFIX + "_FORK_MODE";
    private static final String FORK_MODE_DEFAULT = "false";

    private static final String SERVER_NAME_PROPERTY = HTTP_PROPERTY_PREFIX + "server.name";
    private static final String SERVER_NAME_ENV = HTTP_ENV_PREFIX + "SERVER_NAME";
    private static final String SERVER_NAME_DEFAULT = "yaks-http-server";

    private static final String SERVER_PORT_PROPERTY = HTTP_PROPERTY_PREFIX + "server.port";
    private static final String SERVER_PORT_ENV = HTTP_ENV_PREFIX + "SERVER_PORT";
    private static final String SERVER_PORT_DEFAULT = "8080";

    private HttpSettings() {
        // prevent instantiation of utility class
    }

    /**
     * Request timeout when receiving messages.
     * @return
     */
    public static long getTimeout() {
        return Long.parseLong(System.getProperty(TIMEOUT_PROPERTY,
                System.getenv(TIMEOUT_ENV) != null ? System.getenv(TIMEOUT_ENV) : TIMEOUT_DEFAULT));
    }

    /**
     * Request fork mode when sending messages.
     * @return
     */
    public static boolean getForkMode() {
        return Boolean.parseBoolean(System.getProperty(FORK_MODE_PROPERTY,
                System.getenv(FORK_MODE_ENV) != null ? System.getenv(FORK_MODE_ENV) : FORK_MODE_DEFAULT));
    }

    /**
     * Service name to use when creating a new service for cloud event subscriptions.
     * @return
     */
    public static String getServerName() {
        return System.getProperty(SERVER_NAME_PROPERTY,
                System.getenv(SERVER_NAME_ENV) != null ? System.getenv(SERVER_NAME_ENV) : SERVER_NAME_DEFAULT);
    }

    /**
     * Service port used when consuming cloud events via Http.
     * @return
     */
    public static int getServerPort() {
        return Integer.parseInt(System.getProperty(SERVER_PORT_PROPERTY,
                System.getenv(SERVER_PORT_ENV) != null ? System.getenv(SERVER_PORT_ENV) : SERVER_PORT_DEFAULT));
    }
}
