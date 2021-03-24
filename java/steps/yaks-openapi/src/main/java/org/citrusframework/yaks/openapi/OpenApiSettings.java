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

package org.citrusframework.yaks.openapi;

/**
 * @author Christoph Deppisch
 */
public class OpenApiSettings {

    private static final String OPENAPI_PROPERTY_PREFIX = "yaks.openapi.";
    private static final String OPENAPI_ENV_PREFIX = "YAKS_OPENAPI_";

    private static final String TIMEOUT_PROPERTY = OPENAPI_PROPERTY_PREFIX + ".timeout";
    private static final String TIMEOUT_ENV = OPENAPI_ENV_PREFIX + "_TIMEOUT";
    private static final String TIMEOUT_DEFAULT = "2000";

    private static final String GENERATE_OPTIONAL_FIELDS_PROPERTY = OPENAPI_PROPERTY_PREFIX + "generate.optional.fields";
    private static final String GENERATE_OPTIONAL_FIELDS_ENV = OPENAPI_ENV_PREFIX + "GENERATE_OPTIONAL_FIELDS";
    private static final String GENERATE_OPTIONAL_FIELDS_DEFAULT = "true";

    private static final String VALIDATE_OPTIONAL_FIELDS_PROPERTY = OPENAPI_PROPERTY_PREFIX + "validate.optional.fields";
    private static final String VALIDATE_OPTIONAL_FIELDS_ENV = OPENAPI_ENV_PREFIX + "VALIDATE_OPTIONAL_FIELDS";
    private static final String VALIDATE_OPTIONAL_FIELDS_DEFAULT = "true";

    private OpenApiSettings() {
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
     * Include optional fields when generating test data as request/response body.
     * @return
     */
    public static boolean isGenerateOptionalFields() {
        return Boolean.parseBoolean(System.getProperty(GENERATE_OPTIONAL_FIELDS_PROPERTY,
                System.getenv(GENERATE_OPTIONAL_FIELDS_ENV) != null ? System.getenv(GENERATE_OPTIONAL_FIELDS_ENV) : GENERATE_OPTIONAL_FIELDS_DEFAULT));
    }

    /**
     * Include optional fields when validating test data in request/response body.
     * @return
     */
    public static boolean isValidateOptionalFields() {
        return Boolean.parseBoolean(System.getProperty(VALIDATE_OPTIONAL_FIELDS_PROPERTY,
                System.getenv(VALIDATE_OPTIONAL_FIELDS_ENV) != null ? System.getenv(VALIDATE_OPTIONAL_FIELDS_ENV) : VALIDATE_OPTIONAL_FIELDS_DEFAULT));
    }
}
