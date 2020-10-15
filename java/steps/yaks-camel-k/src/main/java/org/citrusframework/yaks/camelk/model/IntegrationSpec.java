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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"sources", "dependencies", "traits"})
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class IntegrationSpec implements KubernetesResource {

    @JsonProperty("sources")
    private List<Source> sources;
    @JsonProperty("dependencies")
    private List<String> dependencies;
    @JsonProperty("traits")
    private Map<String, TraitConfig> traits;

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Map<String, TraitConfig> getTraits() {
        return traits;
    }

    public void setTraits(Map<String, TraitConfig> traits) {
        this.traits = traits;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"configuration"})
    public static class TraitConfig {
        @JsonProperty("configuration")
        private Map<String, String> configuration;

        public TraitConfig() {
            super();
        }

        public TraitConfig(String key, String value) {
            this.configuration = new HashMap<>();
            add(key, value);
        }

        public Map<String, String> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, String> configuration) {
            this.configuration = configuration;
        }

        public void add(String key, String value) {
            this.configuration.put(key, value);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"name", "content"})
    public static class Source {
        @JsonProperty("content")
        private String content;
        @JsonProperty("name")
        private String name;

        public Source() {
            super();
        }

        public Source(String name, String content) {
            this.content = content;
            this.name = name;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
