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

package org.citrusframework.yaks.testcontainers.aws2;

public class AWSContainerSettings {

    private static final String AWS_PROPERTY_PREFIX = "aws.";

    public static final String ACCESS_KEY_PROPERTY = AWS_PROPERTY_PREFIX + "access.key";
    public static final String SECRET_KEY_PROPERTY = AWS_PROPERTY_PREFIX + "secret.key";
    public static final String REGION_PROPERTY = AWS_PROPERTY_PREFIX + "region";
    public static final String HOST_PROPERTY = AWS_PROPERTY_PREFIX + "host";
    public static final String PROTOCOL_PROPERTY = AWS_PROPERTY_PREFIX + "protocol";
}
