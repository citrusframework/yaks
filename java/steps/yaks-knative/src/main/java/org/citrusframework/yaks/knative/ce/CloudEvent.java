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

package org.citrusframework.yaks.knative.ce;

import java.util.Arrays;
import java.util.List;

/**
 * @author Christoph Deppisch
 */
public class CloudEvent {

    private final String version;
    private final List<Attribute> attributes;

    private CloudEvent(String version, List<Attribute> attributes) {
        this.version = version;
        this.attributes = attributes;
    }

    public String version() {
        return version;
    }

    public List<Attribute> attributes() {
        return attributes;
    }

    /**
     * Create new cloud event for version 1.0
     * https://github.com/cloudevents/spec/blob/v1.0/spec.md
     * @return
     */
    public static CloudEvent v1_0() {
        return new CloudEvent(
                "1.0",
                Arrays.asList(
                        Attribute.create("Ce-Id", "id", "yaks-test-event"),
                        Attribute.create("Ce-Source", "source", "yaks-test-source"),
                        Attribute.create("Ce-Specversion", "specversion", "1.0"),
                        Attribute.create("Ce-Type", "type", "yaks-test"),
                        Attribute.create("Ce-Subject", "subject"),
                        Attribute.create("Ce-Dataschema", "dataschema"),
                        Attribute.create("Ce-Time", "time"),
                        Attribute.create("Content-Type", "datacontenttype")
                )
        );
    }

    /**
     * Cloud event attribute with Http header name and Json field name representation. Optional default value
     * can be specified.
     */
    public interface Attribute {
        /**
         * The name of the http header.
         */
        String http();

        /**
         * The name of the json field.
         */
        String json();

        /**
         * Default value if any.
         */
        String defaultValue();

        static Attribute create(String http, String json) {
            return create(http, json, null);
        }

        static Attribute create(String http, String json, String defaultValue) {
            return new Attribute() {
                @Override
                public String http() {
                    return http;
                }

                @Override
                public String json() {
                    return json;
                }

                @Override
                public String defaultValue() {
                    return defaultValue;
                }
            };
        }
    }
}
