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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"integration", "replicas", "source", "steps", "sink"})
public class BindingSpec implements KubernetesResource {

    @JsonProperty("integration")
    private Integration integration;

    @JsonProperty("source")
    private Endpoint source;

    @JsonProperty("sink")
    private Endpoint sink;

    @JsonProperty("steps")
    private Endpoint[] steps;

    @JsonProperty("replicas")
    private Integer replicas;

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setSource(Endpoint source) {
        this.source = source;
    }

    public Endpoint getSource() {
        return source;
    }

    public void setSink(Endpoint sink) {
        this.sink = sink;
    }

    public Endpoint getSink() {
        return sink;
    }

    public Endpoint[] getSteps() {
        return steps;
    }

    public void setSteps(Endpoint[] steps) {
        this.steps = steps;
    }

    public void setIntegration(Integration integration) {
        this.integration = integration;
    }

    public Integration getIntegration() {
        return integration;
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"ref", "uri", "properties", "dataTypes"})
    public static class Endpoint implements KubernetesResource {
        @JsonProperty("ref")
        private ObjectReference ref;

        @JsonProperty("uri")
        private String uri;

        @JsonProperty("properties")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, Object> properties = new HashMap<>();

        @JsonProperty("dataTypes")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, DataTypeRef> dataTypes = new HashMap<>();

        public Endpoint() {
        }

        public Endpoint(ObjectReference ref) {
            this.ref = ref;
        }

        public Endpoint(ObjectReference ref, Map<String, Object> properties) {
            this.ref = ref;
            this.properties = properties;
        }

        public Endpoint(String uri) {
            this.uri = uri;
        }

        public ObjectReference getRef() {
            return ref;
        }

        public void setRef(ObjectReference ref) {
            this.ref = ref;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public Map<String, DataTypeRef> getDataTypes() {
            return dataTypes;
        }

        public void setDataTypes(Map<String, DataTypeRef> dataTypes) {
            this.dataTypes = dataTypes;
        }

        @JsonDeserialize(using = JsonDeserializer.None.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({"scheme", "format"})
        public static class DataTypeRef implements KubernetesResource {
            @JsonProperty("scheme")
            private String scheme;

            @JsonProperty("format")
            private String format;

            public DataTypeRef() {
                super();
            }

            public DataTypeRef(String scheme, String format) {
                this.scheme = scheme;
                this.format = format;
            }

            public String getScheme() {
                return scheme;
            }

            public void setScheme(String scheme) {
                this.scheme = scheme;
            }

            public String getFormat() {
                return format;
            }

            public void setFormat(String format) {
                this.format = format;
            }
        }

        @JsonDeserialize(using = JsonDeserializer.None.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({"name", "kind", "namespace", "uid", "apiVersion", "resourceVersion", "fieldPath"})
        public static class ObjectReference implements KubernetesResource {
            @JsonProperty("name")
            private String name;
            @JsonProperty("kind")
            private String kind;
            @JsonProperty("namespace")
            private String namespace;
            @JsonProperty("uid")
            private String uid;
            @JsonProperty("apiVersion")
            private String apiVersion;
            @JsonProperty("resourceVersion")
            private String resourceVersion;
            @JsonProperty("fieldPath")
            private String fieldPath;

            public ObjectReference() {
                super();
            }

            public ObjectReference(String apiVersion, String kind, String namespace, String name) {
                this.apiVersion = apiVersion;
                this.kind = kind;
                this.namespace = namespace;
                this.name = name;
            }

            public ObjectReference(String kind, String namespace, String name) {
                this.kind = kind;
                this.namespace = namespace;
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getKind() {
                return kind;
            }

            public void setKind(String kind) {
                this.kind = kind;
            }

            public String getNamespace() {
                return namespace;
            }

            public void setNamespace(String namespace) {
                this.namespace = namespace;
            }

            public String getUid() {
                return uid;
            }

            public void setUid(String uid) {
                this.uid = uid;
            }

            public String getApiVersion() {
                return apiVersion;
            }

            public void setApiVersion(String apiVersion) {
                this.apiVersion = apiVersion;
            }

            public String getResourceVersion() {
                return resourceVersion;
            }

            public void setResourceVersion(String resourceVersion) {
                this.resourceVersion = resourceVersion;
            }

            public String getFieldPath() {
                return fieldPath;
            }

            public void setFieldPath(String fieldPath) {
                this.fieldPath = fieldPath;
            }
        }
    }

}
