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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec", "status"})
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class KameletBinding extends CustomResource {

    @JsonProperty("spec")
    private KameletBindingSpec spec = new KameletBindingSpec();

    @JsonProperty("status")
    private KameletBindingStatus status;

    @Override
    public String getApiVersion() {
        return CamelKSupport.CAMELK_CRD_GROUP + "/" + CamelKSettings.getKameletApiVersion();
    }

    public KameletBindingSpec getSpec() {
        return spec;
    }

    public void setSpec(KameletBindingSpec spec) {
        this.spec = spec;
    }

    /**
     * Fluent builder
     */
    public static class Builder {
        private String name;
        private Integration integration;
        private KameletBindingSpec.Endpoint source;
        private KameletBindingSpec.Endpoint sink;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder integration(Integration integration) {
            this.integration = integration;
            return this;
        }

        public Builder source(KameletBindingSpec.Endpoint source) {
            this.source = source;
            return this;
        }

        public Builder source(String uri) {
            return source(new KameletBindingSpec.Endpoint(uri));
        }

        public Builder source(KameletBindingSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return source(new KameletBindingSpec.Endpoint(ref, props));
        }

        public Builder sink(KameletBindingSpec.Endpoint sink) {
            this.sink = sink;
            return this;
        }

        public Builder sink(String uri) {
            return sink(new KameletBindingSpec.Endpoint(uri));
        }

        public Builder sink(KameletBindingSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return sink(new KameletBindingSpec.Endpoint(ref, props));
        }

        public KameletBinding build() {
            KameletBinding binding = new KameletBinding();
            binding.getMetadata().setName(name);

            if (integration != null) {
                binding.getSpec().setIntegration(integration);
            }

            if (source != null) {
                binding.getSpec().setSource(source);
            }

            if (sink != null) {
                binding.getSpec().setSink(sink);
            }

            return binding;
        }
    }
}
