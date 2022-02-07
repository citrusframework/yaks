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

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author Christoph Deppisch
 */
public class PostgreSQLSettings {

    private static final String POSTGRESQL_PROPERTY_PREFIX = TestContainersSettings.TESTCONTAINERS_PROPERTY_PREFIX + "postgresql.";
    private static final String POSTGRESQL_ENV_PREFIX = TestContainersSettings.TESTCONTAINERS_ENV_PREFIX + "POSTGRESQL_";

    private static final String POSTGRESQL_VERSION_PROPERTY = POSTGRESQL_PROPERTY_PREFIX + "postgresql.version";
    private static final String POSTGRESQL_VERSION_ENV = POSTGRESQL_ENV_PREFIX + "POSTGRESQL_VERSION";
    private static final String POSTGRESQL_VERSION_DEFAULT = PostgreSQLContainer.DEFAULT_TAG;

    private static final String DATABASE_NAME_PROPERTY = POSTGRESQL_PROPERTY_PREFIX + "db.name";
    private static final String DATABASE_NAME_ENV = POSTGRESQL_ENV_PREFIX + "DB_NAME";
    private static final String DATABASE_NAME_DEFAULT = "test";

    private static final String USERNAME_PROPERTY = POSTGRESQL_PROPERTY_PREFIX + "username";
    private static final String USERNAME_ENV = POSTGRESQL_ENV_PREFIX + "USERNAME";
    private static final String USERNAME_DEFAULT = "test";

    private static final String PASSWORD_PROPERTY = POSTGRESQL_PROPERTY_PREFIX + "password";
    private static final String PASSWORD_ENV = POSTGRESQL_ENV_PREFIX + "PASSWORD";
    private static final String PASSWORD_DEFAULT = "test";

    private static final String STARTUP_TIMEOUT_PROPERTY = POSTGRESQL_PROPERTY_PREFIX + "startup.timeout";
    private static final String STARTUP_TIMEOUT_ENV = POSTGRESQL_ENV_PREFIX + "STARTUP_TIMEOUT";
    private static final String STARTUP_TIMEOUT_DEFAULT = "180";

    private PostgreSQLSettings() {
        // prevent instantiation of utility class
    }

    /**
     * PostgreSQL database name.
     * @return default database name.
     */
    public static String getDatabaseName() {
        return System.getProperty(DATABASE_NAME_PROPERTY,
                System.getenv(DATABASE_NAME_ENV) != null ? System.getenv(DATABASE_NAME_ENV) : DATABASE_NAME_DEFAULT);
    }

    /**
     * PostgreSQL user name.
     * @return default user name.
     */
    public static String getUsername() {
        return System.getProperty(USERNAME_PROPERTY,
                System.getenv(USERNAME_ENV) != null ? System.getenv(USERNAME_ENV) : USERNAME_DEFAULT);
    }

    /**
     * PostgreSQL password.
     * @return default password.
     */
    public static String getPassword() {
        return System.getProperty(PASSWORD_PROPERTY,
                System.getenv(PASSWORD_ENV) != null ? System.getenv(PASSWORD_ENV) : PASSWORD_DEFAULT);
    }

    /**
     * PostgreSQL version setting.
     * @return
     */
    public static String getPostgreSQLVersion() {
        return System.getProperty(POSTGRESQL_VERSION_PROPERTY,
                System.getenv(POSTGRESQL_VERSION_ENV) != null ? System.getenv(POSTGRESQL_VERSION_ENV) : POSTGRESQL_VERSION_DEFAULT);
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
