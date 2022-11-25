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

/**
 * @author Christoph Deppisch
 */
public enum YaksVariableNames {

    FEATURE_FILE("FEATURE_FILE"),
    SCENARIO_ID("SCENARIO_ID"),
    SCENARIO_NAME("SCENARIO_NAME"),
    CLUSTER_WILDCARD_DOMAIN("CLUSTER_WILDCARD_DOMAIN"),
    NAMESPACE("YAKS_NAMESPACE"),
    OPERATOR_NAMESPACE("YAKS_OPERATOR_NAMESPACE");

    private final String variableName;

    YaksVariableNames(String variableName) {
        this.variableName = variableName;
    }

    public String value() {
        return variableName;
    }

    @Override
    public String toString() {
        return variableName;
    }
}
