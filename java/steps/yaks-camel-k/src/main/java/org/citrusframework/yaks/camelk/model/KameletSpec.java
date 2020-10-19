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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"definition", "dependencies", "types", "sources", "flow"})
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class KameletSpec implements KubernetesResource {

    @JsonProperty("definition")
    private Definition definition;
    @JsonProperty("dependencies")
    private List<String> dependencies;
    @JsonProperty("types")
    private Map<String, TypeSpec> types;
    @JsonProperty("sources")
    private List<Source> sources;
    @JsonProperty("flow")
    private Map<String, Object> flow;

    public Definition getDefinition() {
        return definition;
    }

    public void setDefinition(Definition definition) {
        this.definition = definition;
    }

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

    public Map<String, TypeSpec> getTypes() {
        return types;
    }

    public void setTypes(Map<String, TypeSpec> types) {
        this.types = types;
    }

    public Map<String, Object> getFlow() {
        return flow;
    }

    public void setFlow(Map<String, Object> flow) {
        this.flow = flow;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"mediaType", "schema"})
    public static class TypeSpec {
        @JsonProperty("mediaType")
        private String mediaType;
        @JsonProperty("schema")
        private ObjectSchema schema;

        public TypeSpec() {
            super();
        }

        public TypeSpec(String mediaType) {
            this(mediaType, null);
        }

        public TypeSpec(String mediaType, ObjectSchema schema) {
            this.mediaType = mediaType;
            this.schema = schema;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public JsonSchema getSchema() {
            return schema;
        }

        public void setSchema(ObjectSchema schema) {
            this.schema = schema;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"title", "description", "required", "properties"})
    public static class Definition {
        @JsonProperty("title")
        private String title;
        @JsonProperty("description")
        private String description;
        @JsonProperty("required")
        private List<String> required = new ArrayList<>();
        @JsonProperty("properties")
        private Map<String, PropertyConfig> properties = new HashMap<>();

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }

        public Map<String, PropertyConfig> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, PropertyConfig> properties) {
            this.properties = properties;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonPropertyOrder({"title", "description", "type", "default", "example"})
        public static class PropertyConfig {
            @JsonProperty("title")
            private String title;
            @JsonProperty("description")
            private String description;
            @JsonProperty("type")
            private String type;
            @JsonProperty("default")
            private Object defaultValue;
            @JsonProperty("example")
            private Object example;

            public PropertyConfig() {
                super();
            }

            public PropertyConfig(String title, String type, Object defaultValue, Object example) {
                this.title = title;
                this.type = type;
                this.defaultValue = defaultValue;
                this.example = example;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Object getDefault() {
                return defaultValue;
            }

            public void setDefault(Object defaultValue) {
                this.defaultValue = defaultValue;
            }

            public Object getExample() {
                return example;
            }

            public void setExample(Object example) {
                this.example = example;
            }
        }
    }
}
