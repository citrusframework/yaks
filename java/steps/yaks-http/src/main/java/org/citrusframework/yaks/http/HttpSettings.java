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

package org.citrusframework.yaks.http;

/**
 * @author Christoph Deppisch
 */
public class HttpSettings {

    private static final String HTTP_PROPERTY_PREFIX = "yaks.http.";
    private static final String HTTP_ENV_PREFIX = "YAKS_HTTP_";

    private static final String TIMEOUT_PROPERTY = HTTP_PROPERTY_PREFIX + "timeout";
    private static final String TIMEOUT_ENV = HTTP_ENV_PREFIX + "TIMEOUT";
    private static final String TIMEOUT_DEFAULT = "2000";

    private static final String SERVER_AUTH_PATH_PROPERTY = HTTP_PROPERTY_PREFIX + "server.auth.path";
    private static final String SERVER_AUTH_PATH_ENV = HTTP_ENV_PREFIX + "SERVER_AUTH_PATH";
    private static final String SERVER_AUTH_PATH_DEFAULT = "/secure/*";

    private static final String SERVER_AUTH_USER_ROLES_PROPERTY = HTTP_PROPERTY_PREFIX + "server.auth.user.roles";
    private static final String SERVER_AUTH_USER_ROLES_ENV = HTTP_ENV_PREFIX + "SERVER_AUTH_USER_ROLES";
    private static final String SERVER_AUTH_USER_ROLES_DEFAULT = "citrus";

    private static final String AUTH_METHOD_PROPERTY = HTTP_PROPERTY_PREFIX + "auth.method";
    private static final String AUTH_METHOD_ENV = HTTP_ENV_PREFIX + "AUTH_METHOD";
    private static final String AUTH_METHOD_DEFAULT = "none";

    private static final String SERVER_AUTH_METHOD_PROPERTY = HTTP_PROPERTY_PREFIX + "server.auth.method";
    private static final String SERVER_AUTH_METHOD_ENV = HTTP_ENV_PREFIX + "SERVER_AUTH_METHOD";

    private static final String CLIENT_AUTH_METHOD_PROPERTY = HTTP_PROPERTY_PREFIX + "client.auth.method";
    private static final String CLIENT_AUTH_METHOD_ENV = HTTP_ENV_PREFIX + "CLIENT_AUTH_METHOD";

    private static final String AUTH_USER_PROPERTY = HTTP_PROPERTY_PREFIX + "auth.user";
    private static final String AUTH_USER_ENV = HTTP_ENV_PREFIX + "AUTH_USER";
    private static final String AUTH_USER_DEFAULT = "citrus";

    private static final String SERVER_AUTH_USER_PROPERTY = HTTP_PROPERTY_PREFIX + "server.auth.user";
    private static final String SERVER_AUTH_USER_ENV = HTTP_ENV_PREFIX + "SERVER_AUTH_USER";

    private static final String CLIENT_AUTH_USER_PROPERTY = HTTP_PROPERTY_PREFIX + "client.auth.user";
    private static final String CLIENT_AUTH_USER_ENV = HTTP_ENV_PREFIX + "CLIENT_AUTH_USER";

    private static final String AUTH_PASSWORD_PROPERTY = HTTP_PROPERTY_PREFIX + "auth.password";
    private static final String AUTH_PASSWORD_ENV = HTTP_ENV_PREFIX + "AUTH_PASSWORD";
    private static final String AUTH_PASSWORD_DEFAULT = "secr3t";

    private static final String SERVER_AUTH_PASSWORD_PROPERTY = HTTP_PROPERTY_PREFIX + "server.auth.password";
    private static final String SERVER_AUTH_PASSWORD_ENV = HTTP_ENV_PREFIX + "SERVER_AUTH_PASSWORD";

    private static final String CLIENT_AUTH_PASSWORD_PROPERTY = HTTP_PROPERTY_PREFIX + "client.auth.password";
    private static final String CLIENT_AUTH_PASSWORD_ENV = HTTP_ENV_PREFIX + "CLIENT_AUTH_PASSWORD";

    private static final String FORK_MODE_PROPERTY = HTTP_PROPERTY_PREFIX + "fork.mode";
    private static final String FORK_MODE_ENV = HTTP_ENV_PREFIX + "FORK_MODE";
    private static final String FORK_MODE_DEFAULT = "false";

    private static final String SERVER_NAME_PROPERTY = HTTP_PROPERTY_PREFIX + "server.name";
    private static final String SERVER_NAME_ENV = HTTP_ENV_PREFIX + "SERVER_NAME";
    private static final String SERVER_NAME_DEFAULT = "yaks-http-server";

    private static final String SERVER_PORT_PROPERTY = HTTP_PROPERTY_PREFIX + "server.port";
    private static final String SERVER_PORT_ENV = HTTP_ENV_PREFIX + "SERVER_PORT";
    private static final String SERVER_PORT_DEFAULT = "8080";

    private static final String SECURE_PORT_PROPERTY = HTTP_PROPERTY_PREFIX + "secure.port";
    private static final String SECURE_PORT_ENV = HTTP_ENV_PREFIX + "SECURE_PORT";
    private static final String SECURE_PORT_DEFAULT = "8443";

    private static final String SECURE_KEYSTORE_PATH_PROPERTY = HTTP_PROPERTY_PREFIX + "secure.keystore.path";
    private static final String SECURE_KEYSTORE_PATH_ENV = HTTP_ENV_PREFIX + "SECURE_KEYSTORE_PATH";
    static final String SECURE_KEYSTORE_PATH_DEFAULT = "classpath:keystore/http-server.jks";

    private static final String SECURE_KEYSTORE_PASSWORD_PROPERTY = HTTP_PROPERTY_PREFIX + "secure.keystore.password";
    private static final String SECURE_KEYSTORE_PASSWORD_ENV = HTTP_ENV_PREFIX + "SECURE_KEYSTORE_PASSWORD";
    private static final String SECURE_KEYSTORE_PASSWORD_DEFAULT = "secret";

    private static final String HEADER_NAME_IGNORE_CASE_PROPERTY = HTTP_PROPERTY_PREFIX + "header.name.ignore.case";
    private static final String HEADER_NAME_IGNORE_CASE_ENV = HTTP_ENV_PREFIX + "HEADER_NAME_IGNORE_CASE";
    private static final String HEADER_NAME_IGNORE_CASE_DEFAULT = "false";

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

    /**
     * Secure port used when consuming cloud events via Https.
     * @return
     */
    public static int getSecurePort() {
        return Integer.parseInt(System.getProperty(SECURE_PORT_PROPERTY,
                System.getenv(SECURE_PORT_ENV) != null ? System.getenv(SECURE_PORT_ENV) : SECURE_PORT_DEFAULT));
    }

    /**
     * SSL key store path.
     * @return
     */
    public static String getSslKeyStorePath() {
        return System.getProperty(SECURE_KEYSTORE_PATH_PROPERTY,
                System.getenv(SECURE_KEYSTORE_PATH_ENV) != null ? System.getenv(SECURE_KEYSTORE_PATH_ENV) :
                        SECURE_KEYSTORE_PATH_DEFAULT);
    }

    /**
     * SSL key store password.
     * @return
     */
    public static String getSslKeyStorePassword() {
        return System.getProperty(SECURE_KEYSTORE_PASSWORD_PROPERTY,
                System.getenv(SECURE_KEYSTORE_PASSWORD_ENV) != null ? System.getenv(SECURE_KEYSTORE_PASSWORD_ENV) :
                        SECURE_KEYSTORE_PASSWORD_DEFAULT);
    }

    public static boolean isHeaderNameIgnoreCase() {
        return Boolean.parseBoolean(System.getProperty(HEADER_NAME_IGNORE_CASE_PROPERTY,
                System.getenv(HEADER_NAME_IGNORE_CASE_ENV) != null ? System.getenv(HEADER_NAME_IGNORE_CASE_ENV) :
                        HEADER_NAME_IGNORE_CASE_DEFAULT));
    }

    public static String getServerAuthPath() {
        return System.getProperty(SERVER_AUTH_PATH_PROPERTY,
                System.getenv(SERVER_AUTH_PATH_ENV) != null ? System.getenv(SERVER_AUTH_PATH_ENV) :
                        SERVER_AUTH_PATH_DEFAULT);
    }

    public static String[] getServerAuthUserRoles() {
        return System.getProperty(SERVER_AUTH_USER_ROLES_PROPERTY,
                System.getenv(SERVER_AUTH_USER_ROLES_ENV) != null ? System.getenv(SERVER_AUTH_USER_ROLES_ENV) :
                        SERVER_AUTH_USER_ROLES_DEFAULT).split(",");
    }

    public static String getAuthMethod() {
        return System.getProperty(AUTH_METHOD_PROPERTY,
                System.getenv(AUTH_METHOD_ENV) != null ? System.getenv(AUTH_METHOD_ENV) :
                        AUTH_METHOD_DEFAULT);
    }

    public static String getClientAuthMethod() {
        return System.getProperty(CLIENT_AUTH_METHOD_PROPERTY,
                System.getenv(CLIENT_AUTH_METHOD_ENV) != null ? System.getenv(CLIENT_AUTH_METHOD_ENV) :
                        getAuthMethod());
    }

    public static String getServerAuthMethod() {
        return System.getProperty(SERVER_AUTH_METHOD_PROPERTY,
                System.getenv(SERVER_AUTH_METHOD_ENV) != null ? System.getenv(SERVER_AUTH_METHOD_ENV) :
                        getAuthMethod());
    }

    public static String getAuthUser() {
        return System.getProperty(AUTH_USER_PROPERTY,
                System.getenv(AUTH_USER_ENV) != null ? System.getenv(AUTH_USER_ENV) :
                        AUTH_USER_DEFAULT);
    }

    public static String getClientAuthUser() {
        return System.getProperty(CLIENT_AUTH_USER_PROPERTY,
                System.getenv(CLIENT_AUTH_USER_ENV) != null ? System.getenv(CLIENT_AUTH_USER_ENV) :
                        getAuthUser());
    }

    public static String getServerAuthUser() {
        return System.getProperty(SERVER_AUTH_USER_PROPERTY,
                System.getenv(SERVER_AUTH_USER_ENV) != null ? System.getenv(SERVER_AUTH_USER_ENV) :
                        getAuthUser());
    }

    public static String getAuthPassword() {
        return System.getProperty(AUTH_PASSWORD_PROPERTY,
                System.getenv(AUTH_PASSWORD_ENV) != null ? System.getenv(AUTH_PASSWORD_ENV) :
                        AUTH_PASSWORD_DEFAULT);
    }

    public static String getClientAuthPassword() {
        return System.getProperty(CLIENT_AUTH_PASSWORD_PROPERTY,
                System.getenv(CLIENT_AUTH_PASSWORD_ENV) != null ? System.getenv(CLIENT_AUTH_PASSWORD_ENV) :
                        getAuthPassword());
    }

    public static String getServerAuthPassword() {
        return System.getProperty(SERVER_AUTH_PASSWORD_PROPERTY,
                System.getenv(SERVER_AUTH_PASSWORD_ENV) != null ? System.getenv(SERVER_AUTH_PASSWORD_ENV) :
                        getAuthPassword());
    }
}
