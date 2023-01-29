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

@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"replicas", "flows", "sources", "resources", "kit", "dependencies", "profile", "traits",
        "configuration", "repositories", "serviceAccountName"})
public class IntegrationSpec implements KubernetesResource {

    @JsonProperty("replicas")
    private Integer replicas;
    @JsonProperty("flows")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Map<String, Object>> flows;
    @JsonProperty("sources")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Source> sources;
    @JsonProperty("resources")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Resource> resources;
    @JsonProperty("kit")
    private String kit;
    @JsonProperty("dependencies")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> dependencies;
    @JsonProperty("profile")
    private String profile;
    @JsonProperty("traits")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, TraitConfig> traits;
    @JsonProperty("configuration")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Configuration> configuration;
    @JsonProperty("repositories")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> repositories;
    @JsonProperty("serviceAccountName")
    private String serviceAccountName;

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public List<Map<String, Object>> getFlows() {
        return flows;
    }

    public void setFlows(List<Map<String, Object>> flows) {
        this.flows = flows;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public String getKit() {
        return kit;
    }

    public void setKit(String kit) {
        this.kit = kit;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public Map<String, TraitConfig> getTraits() {
        return traits;
    }

    public void setTraits(Map<String, TraitConfig> traits) {
        this.traits = traits;
    }

    public List<Configuration> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(List<Configuration> configuration) {
        this.configuration = configuration;
    }

    public List<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<String> repositories) {
        this.repositories = repositories;
    }

    public String getServiceAccountName() {
        return serviceAccountName;
    }

    public void setServiceAccountName(String serviceAccountName) {
        this.serviceAccountName = serviceAccountName;
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"type", "value"})
    public static class Configuration implements KubernetesResource {
        @JsonProperty("type")
        private String type;
        @JsonProperty("value")
        private String value;

        public Configuration() {
            super();
        }

        public Configuration(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String defaultValue) {
            this.value = defaultValue;
        }
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"configuration"})
    public static class TraitConfig implements KubernetesResource {
        @JsonProperty("configuration")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> configuration = new HashMap<>();

        public TraitConfig() {
            // default constructor
        }

        public TraitConfig(String key, Object value) {
            add(key, value);
        }

        public Map<String, Object> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, Object> configuration) {
            this.configuration = configuration;
        }

        public void add(String key, Object value) {
            this.configuration.put(key, value);
        }
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"language", "loader", "type", "name", "path", "rawContent", "content",
            "contentType", "contentRef", "contentKey", "compression", "property-names", "interceptors"})
    public static class Source extends DataSpec implements KubernetesResource {
        @JsonProperty("language")
        private String language;
        @JsonProperty("loader")
        private String loader;
        @JsonProperty("type")
        private String type;
        @JsonProperty("interceptors")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> interceptors;
        @JsonProperty("property-names")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> propertyNames;

        public Source() {
            super();
        }

        public Source(String name, String content) {
            super(name, content);
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getLoader() {
            return loader;
        }

        public void setLoader(String loader) {
            this.loader = loader;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getInterceptors() {
            return interceptors;
        }

        public void setInterceptors(List<String> interceptors) {
            this.interceptors = interceptors;
        }

        public List<String> getPropertyNames() {
            return propertyNames;
        }

        public void setPropertyNames(List<String> propertyNames) {
            this.propertyNames = propertyNames;
        }
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"type", "mountPath", "name", "path", "rawContent", "content",
            "contentType", "contentRef", "contentKey", "compression"})
    public static class Resource extends DataSpec implements KubernetesResource {
        @JsonProperty("type")
        private String type;
        @JsonProperty("mountPath")
        private String mountPath;

        public Resource() {
            super();
        }

        public Resource(String type, String mountPath, String name, String content) {
            super(name, content);
            this.mountPath = mountPath;
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMountPath() {
            return mountPath;
        }

        public void setMountPath(String mountPath) {
            this.mountPath = mountPath;
        }
    }

    private static class DataSpec {
        @JsonProperty("name")
        private String name;
        @JsonProperty("path")
        private String path;
        @JsonProperty("content")
        private String content;
        @JsonProperty("contentRef")
        private String contentRef;
        @JsonProperty("contentType")
        private String contentType;
        @JsonProperty("contentKey")
        private String contentKey;
        @JsonProperty("compression")
        private String compression;
        @JsonProperty("rawContent")
        private String rawContent;

        public DataSpec() {
            super();
        }

        public DataSpec(String name, String content) {
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

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getContentRef() {
            return contentRef;
        }

        public void setContentRef(String contentRef) {
            this.contentRef = contentRef;
        }

        public String getContentKey() {
            return contentKey;
        }

        public void setContentKey(String contentKey) {
            this.contentKey = contentKey;
        }

        public String getCompression() {
            return compression;
        }

        public void setCompression(String compression) {
            this.compression = compression;
        }

        public String getRawContent() {
            return rawContent;
        }

        public void setRawContent(String rawContent) {
            this.rawContent = rawContent;
        }
    }
}
