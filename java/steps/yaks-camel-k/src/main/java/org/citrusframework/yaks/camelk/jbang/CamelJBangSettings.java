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

package org.citrusframework.yaks.camelk.jbang;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Christoph Deppisch
 */
public final class CamelJBangSettings {

    private static final String JBANG_PROPERTY_PREFIX = "yaks.jbang.";
    private static final String JBANG_ENV_PREFIX = "YAKS_JBANG_";

    private static final String CAMEL_APP_PROPERTY = JBANG_PROPERTY_PREFIX + "camel.app";
    private static final String CAMEL_APP_ENV = JBANG_ENV_PREFIX + "CAMEL_APP";
    private static final String CAMEL_APP_DEFAULT = "camel@apache/camel";

    private static final String CAMEL_VERSION_PROPERTY = JBANG_PROPERTY_PREFIX + "camel.version";
    private static final String CAMEL_VERSION_ENV = JBANG_ENV_PREFIX + "CAMEL_VERSION";
    private static final String CAMEL_VERSION_DEFAULT = "3.20.4";

    private static final String KAMELETS_LOCAL_DIR_PROPERTY = JBANG_PROPERTY_PREFIX + "kamelets.local.dir";
    private static final String KAMELETS_LOCAL_DIR_ENV = JBANG_ENV_PREFIX + "KAMELETS_LOCAL_DIR";

    private static final String TRUST_URL_PROPERTY = JBANG_PROPERTY_PREFIX + "trust.url";
    private static final String TRUST_URL_ENV = JBANG_ENV_PREFIX + "TRUST_URL";
    private static final String TRUST_URL_DEFAULT = "https://github.com/apache/camel/";

    private static final String JBANG_DOWNLOAD_URL_PROPERTY = JBANG_PROPERTY_PREFIX + "download.url";
    private static final String JBANG_DOWNLOAD_URL_ENV = JBANG_ENV_PREFIX + "DOWNLOAD_URL";
    private static final String JBANG_DOWNLOAD_URL_DEFAULT = "https://jbang.dev/releases/latest/download/jbang.zip";

    private static final String WORK_DIR_PROPERTY = JBANG_PROPERTY_PREFIX + "work.dir";
    private static final String WORK_DIR_ENV = JBANG_ENV_PREFIX + "WORK_DIR";
    private static final String WORK_DIR_DEFAULT = ".yaks-jbang";

    private static final String DUMP_PROCESS_OUTPUT_PROPERTY = JBANG_PROPERTY_PREFIX + "dump.process.output";
    private static final String DUMP_PROCESS_OUTPUT_ENV = JBANG_ENV_PREFIX + "DUMP_PROCESS_OUTPUT";
    private static final String DUMP_PROCESS_OUTPUT_DEFAULT = "false";

    private static final String CAMEL_DUMP_INTEGRATION_OUTPUT_PROPERTY = JBANG_PROPERTY_PREFIX + "camel.dump.integration.output";
    private static final String CAMEL_DUMP_INTEGRATION_OUTPUT_ENV = JBANG_ENV_PREFIX + "CAMEL_DUMP_INTEGRATION_OUTPUT";
    private static final String CAMEL_DUMP_INTEGRATION_OUTPUT_DEFAULT = "false";

    private CamelJBangSettings() {
        // prevent instantiation of utility class
    }

    /**
     * JBang download url.
     * @return
     */
    public static String getJBangDownloadUrl() {
        return System.getProperty(JBANG_DOWNLOAD_URL_PROPERTY,
                System.getenv(JBANG_DOWNLOAD_URL_ENV) != null ? System.getenv(JBANG_DOWNLOAD_URL_ENV) : JBANG_DOWNLOAD_URL_DEFAULT);
    }

    /**
     * JBang local work dir.
     * @return
     */
    public static Path getWorkDir() {
        String workDir = Optional.ofNullable(System.getProperty(WORK_DIR_PROPERTY, System.getenv(WORK_DIR_ENV))).orElse(WORK_DIR_DEFAULT);

        Path path = Paths.get(workDir);
        if (path.isAbsolute()) {
            return path.toAbsolutePath();
        } else {
            return Paths.get("").toAbsolutePath().resolve(workDir).toAbsolutePath();
        }
    }

    /**
     * JBang local Kamelets dir.
     * @return
     */
    public static Path getKameletsLocalDir() {
        return Optional.ofNullable(System.getProperty(KAMELETS_LOCAL_DIR_PROPERTY, System.getenv(KAMELETS_LOCAL_DIR_ENV))).map(dir -> {
            Path path = Paths.get(dir);
            if (path.isAbsolute()) {
                return path.toAbsolutePath();
            } else {
                return getWorkDir().resolve(dir).toAbsolutePath();
            }
        }).orElse(null);
    }

    /**
     * JBang trust URLs.
     * @return
     */
    public static String[] getTrustUrl() {
        return System.getProperty(TRUST_URL_PROPERTY,
                System.getenv(TRUST_URL_ENV) != null ? System.getenv(TRUST_URL_ENV) : TRUST_URL_DEFAULT).split(",");
    }

    /**
     * When set to true JBang process output for Camel integrations will be redirected to a file in the current working directory.
     * @return
     */
    public static boolean isCamelDumpIntegrationOutput() {
        return Boolean.parseBoolean(System.getProperty(CAMEL_DUMP_INTEGRATION_OUTPUT_PROPERTY,
                System.getenv(CAMEL_DUMP_INTEGRATION_OUTPUT_ENV) != null ? System.getenv(CAMEL_DUMP_INTEGRATION_OUTPUT_ENV) : CAMEL_DUMP_INTEGRATION_OUTPUT_DEFAULT));
    }

    /**
     * When set to true JBang process output will be redirected to a file in the current working directory.
     * @return
     */
    public static boolean isDumpProcessOutput() {
        return Boolean.parseBoolean(System.getProperty(DUMP_PROCESS_OUTPUT_PROPERTY,
                System.getenv(DUMP_PROCESS_OUTPUT_ENV) != null ? System.getenv(DUMP_PROCESS_OUTPUT_ENV) : DUMP_PROCESS_OUTPUT_DEFAULT));
    }

    /**
     * Camel JBang app name.
     * @return
     */
    public static String getCamelApp() {
        return System.getProperty(CAMEL_APP_PROPERTY,
                System.getenv(CAMEL_APP_ENV) != null ? System.getenv(CAMEL_APP_ENV) : CAMEL_APP_DEFAULT);
    }

    /**
     * Camel JBang version.
     * @return
     */
    public static String getCamelVersion() {
        return System.getProperty(CAMEL_VERSION_PROPERTY,
                System.getenv(CAMEL_VERSION_ENV) != null ? System.getenv(CAMEL_VERSION_ENV) : CAMEL_VERSION_DEFAULT);
    }

}
