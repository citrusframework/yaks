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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec"})
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class Kamelet extends CustomResource {

    @JsonProperty("spec")
    private KameletSpec spec = new KameletSpec();

    @JsonProperty("status")
    private KameletStatus status;

    @Override
    public String getApiVersion() {
        return CamelKSupport.CAMELK_CRD_GROUP + "/" + CamelKSettings.getKameletApiVersion();
    }

    public KameletSpec getSpec() {
        return spec;
    }

    public void setSpec(KameletSpec spec) {
        this.spec = spec;
    }

    /**
     * Fluent builder
     */
    public static class Builder {
        private String name;
        private String flow;
        private KameletSpec.Definition definition = new KameletSpec.Definition();
        private Map<String, KameletSpec.TypeSpec> types = new HashMap<>();
        private List<String> dependencies = new ArrayList<>();
        private KameletSpec.Source source;

        public Builder name(String name) {
            this.name = name;
            definition.setTitle(StringUtils.capitalize(name));
            return this;
        }

        public Builder definition(KameletSpec.Definition definition) {
            this.definition = definition;
            return this;
        }

        public Builder source(String name, String language, String content) {
            this.source = new KameletSpec.Source(name + "." + language, content);
            return this;
        }

        public Builder source(String name, String content) {
            this.source = new KameletSpec.Source(name, content);
            return this;
        }

        public Builder flow(String flow) {
            this.flow = flow;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = Collections.unmodifiableList(dependencies);
            return this;
        }

        public Builder types(Map<String, KameletSpec.TypeSpec> types) {
            this.types.putAll(types);
            return this;
        }

        public Builder addType(String slot, String mediaType) {
            this.types.put(slot, new KameletSpec.TypeSpec(mediaType));
            return this;
        }

        public Kamelet build() {
            Kamelet kamelet = new Kamelet();
            kamelet.getMetadata().setName(name);
            kamelet.getSpec().setDefinition(definition);

            if (flow != null && !flow.isEmpty()) {
                kamelet.getSpec().setFlow(CamelKSupport.yaml().load(flow));
            }

            if (source != null) {
                kamelet.getSpec().setSources(Collections.singletonList(source));
            }

            kamelet.getSpec().setDependencies(dependencies);
            kamelet.getSpec().setTypes(types);
            return kamelet;
        }
    }
}
