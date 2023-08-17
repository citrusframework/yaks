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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(CamelKSupport.CAMELK_CRD_GROUP)
@Version(CamelKSettings.V1)
public class Kamelet extends CustomResource<KameletSpec, KameletStatus> implements Namespaced {

    public Kamelet() {
        super();
        this.spec = new KameletSpec();
        this.status = null;
    }

    /**
     * Fluent builder
     */
    public static class Builder {
        protected String name;
        private String template;
        private KameletSpec.Definition definition = new KameletSpec.Definition();
        private final Map<String, KameletSpec.DataTypesSpec> dataTypes = new HashMap<>();
        private List<String> dependencies = new ArrayList<>();
        private KameletSpec.Source source;

        private final Map<String, String> labels = new LinkedHashMap<>();
        private final Map<String, String> annotations = new LinkedHashMap<>();

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

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        @Deprecated
        public Builder flow(String flow) {
            this.template = flow;
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = Collections.unmodifiableList(dependencies);
            return this;
        }

        public Builder dataTypes(Map<String, KameletSpec.DataTypesSpec> types) {
            this.dataTypes.putAll(types);
            return this;
        }

        public Builder addDataType(String slot, String scheme, String format) {
            if (dataTypes.containsKey(slot)) {
                this.dataTypes.get(slot).getTypes().put(format, new KameletSpec.DataTypeSpec(scheme, format));
            } else {
                this.dataTypes.put(slot, new KameletSpec.DataTypesSpec(format, new KameletSpec.DataTypeSpec(scheme, format)));
            }

            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels.putAll(labels);
            return this;
        }

        public Builder addLabel(String name, String value) {
            this.labels.put(name, value);
            return this;
        }

        public Builder annotations(Map<String, String> annotations) {
            this.annotations.putAll(annotations);
            return this;
        }

        public Builder addAnnotation(String name, String value) {
            this.annotations.put(name, value);
            return this;
        }

        public Kamelet build() {
            Kamelet kamelet = new Kamelet();
            kamelet.getMetadata().setName(name);
            kamelet.getSpec().setDefinition(definition);

            kamelet.getMetadata().getAnnotations().putAll(annotations);
            kamelet.getMetadata().getLabels().putAll(labels);

            if (template != null && !template.isEmpty()) {
                kamelet.getSpec().setTemplate(KubernetesSupport.yaml().load(template));
            }

            if (source != null) {
                kamelet.getSpec().setSources(Collections.singletonList(source));
            }

            kamelet.getSpec().setDependencies(dependencies);
            kamelet.getSpec().setDataTypes(dataTypes);
            return kamelet;
        }
    }
}
