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

package org.citrusframework.yaks.camelk.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.KubernetesResource;

public class IntegrationSpec implements KubernetesResource {

    List<Source> sources;
    List<String> dependencies;
    Map<String, TraitConfig> traits;

    public List<Source> getSources() {
        return sources;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public Map<String, TraitConfig> getTraits() {
        return traits;
    }

    public static class TraitConfig {
        Map<String,String> configuration;

        public TraitConfig(String key, String value) {
            this.configuration = new HashMap<>();
            add(key, value);
        }

        public Map<String, String> getConfiguration() {
            return configuration;
        }

        public String add(String key, String value) {
            return configuration.put(key, value);
        }
    }

    public static class Source {
        String content;
        String name;

        public Source(String name, String content) {
            this.content = content;
            this.name = name;
        }

        public String getContent() {
            return content;
        }

        public String getName() {
            return name;
        }
    }
}
