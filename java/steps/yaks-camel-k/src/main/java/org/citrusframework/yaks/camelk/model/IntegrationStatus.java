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

package org.citrusframework.yaks.camelk.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"phase", "digest", "image", "dependencies", "profile", "integrationKit", "kit", "lastInitTimestamp", "platform", "generatedSources", "generatedResources",
        "failure", "runtimeProvider", "configuration", "conditions", "version", "replicas", "selector", "capabilities", "observedGeneration"})
public class IntegrationStatus implements KubernetesResource {

    @JsonProperty("phase")
    private String phase;
    @JsonProperty("digest")
    private String digest;
    @JsonProperty("image")
    private String image;
    @JsonProperty("dependencies")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> dependencies;
    @JsonProperty("profile")
    private String profile;
    @JsonProperty("integrationKit")
    private IntegrationKit integrationKit;
    @JsonProperty("kit")
    private String kit;
    @JsonProperty("lastInitTimestamp")
    private String lastInitTimestamp;
    @JsonProperty("platform")
    private String platform;
    @JsonProperty("generatedSources")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Source> generatedSources;
    @JsonProperty("generatedResources")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Resource> generatedResources;
    @JsonProperty("failure")
    private String failure;
    @JsonProperty("runtimeVersion")
    private String runtimeVersion;
    @JsonProperty("runtimeProvider")
    private String runtimeProvider;
    @JsonProperty("configuration")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Configuration> configuration;
    @JsonProperty("conditions")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Condition> conditions;
    @JsonProperty("version")
    private String version;
    @JsonProperty("replicas")
    private int replicas;
    @JsonProperty("selector")
    private String selector;
    @JsonProperty("capabilities")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> capabilities;
    @JsonProperty("observedGeneration")
    private Integer observedGeneration;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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

    @Deprecated
    public String getKit() {
        return kit;
    }

    @Deprecated
    public void setKit(String kit) {
        this.kit = kit;
    }

    public IntegrationKit getIntegrationKit() {
        return integrationKit;
    }

    public void setIntegrationKit(IntegrationKit integrationKit) {
        this.integrationKit = integrationKit;
    }

    public String getLastInitTimestamp() {
        return lastInitTimestamp;
    }

    public void setLastInitTimestamp(String lastInitTimestamp) {
        this.lastInitTimestamp = lastInitTimestamp;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<Source> getGeneratedSources() {
        return generatedSources;
    }

    public void setGeneratedSources(List<Source> generatedSources) {
        this.generatedSources = generatedSources;
    }

    public List<Resource> getGeneratedResources() {
        return generatedResources;
    }

    public void setGeneratedResources(List<Resource> generatedResources) {
        this.generatedResources = generatedResources;
    }

    public String getFailure() {
        return failure;
    }

    public void setFailure(String failure) {
        this.failure = failure;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public String getRuntimeProvider() {
        return runtimeProvider;
    }

    public void setRuntimeProvider(String runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
    }

    public List<Configuration> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(List<Configuration> configuration) {
        this.configuration = configuration;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Integer getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Integer observedGeneration) {
        this.observedGeneration = observedGeneration;
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"type", "status", "lastUpdateTime", "lastTransitionTime", "reason", "message"})
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition implements KubernetesResource {
        @JsonProperty("type")
        private String type;
        @JsonProperty("status")
        private String status;
        @JsonProperty("lastUpdateTime")
        private String lastUpdateTime;
        @JsonProperty("lastTransitionTime")
        private String lastTransitionTime;
        @JsonProperty("reason")
        private String reason;
        @JsonProperty("message")
        private String message;

        public Condition() {
            super();
        }

        public Condition(String type, String status, String reason, String message) {
            this.type = type;
            this.status = status;
            this.reason = reason;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(String lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getLastTransitionTime() {
            return lastTransitionTime;
        }

        public void setLastTransitionTime(String lastTransitionTime) {
            this.lastTransitionTime = lastTransitionTime;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
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
            this.value = value;
        }
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"language", "loader", "type", "name", "content", "contentRef", "contentKey", "compression",
            "property-names", "interceptors"})
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
    @JsonPropertyOrder({"type", "mountPath", "name", "content", "contentRef", "contentKey", "compression"})
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
        @JsonProperty("content")
        private String content;
        @JsonProperty("contentRef")
        private String contentRef;
        @JsonProperty("contentKey")
        private String contentKey;
        @JsonProperty("compression")
        private String compression;

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
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"apiVersion", "fieldPath", "kind", "name", "namespace", "resourceVersion", "uid"})
    public static class IntegrationKit implements KubernetesResource {
        @JsonProperty("apiVersion")
        private String apiVersion;
        @JsonProperty("fieldPath")
        private String fieldPath;
        @JsonProperty("kind")
        private String kind;
        @JsonProperty("name")
        private String name;
        @JsonProperty("namespace")
        private String namespace;
        @JsonProperty("resourceVersion")
        private String resourceVersion;
        @JsonProperty("uid")
        private String uid;

        public IntegrationKit() {
            super();
        }

        public IntegrationKit(String apiVersion, String kind, String name) {
            this.apiVersion = apiVersion;
            this.kind = kind;
            this.name = name;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public String getFieldPath() {
            return fieldPath;
        }

        public void setFieldPath(String fieldPath) {
            this.fieldPath = fieldPath;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getResourceVersion() {
            return resourceVersion;
        }

        public void setResourceVersion(String resourceVersion) {
            this.resourceVersion = resourceVersion;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }
    }
}
