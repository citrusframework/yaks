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

package org.citrusframework.yaks.camelk.model.v1alpha1;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletSpec;

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(CamelKSupport.CAMELK_CRD_GROUP)
@Kind("Kamelet")
@Version(CamelKSettings.V1ALPHA1)
public class KameletV1Alpha1 extends Kamelet implements Namespaced {

    public KameletV1Alpha1() {
        super();
        this.spec = new KameletSpec();
        this.status = null;
    }

    /**
     * Fluent builder
     */
    public static class Builder extends Kamelet.Builder {

        Kamelet.Builder delegate = new Kamelet.Builder();

        public Builder name(String name) {
            this.name = name;
            delegate.name(name);
            return this;
        }

        public Builder definition(KameletSpec.Definition definition) {
            delegate.definition(definition);
            return this;
        }

        public Builder source(String name, String language, String content) {
            delegate.source(name, language, content);
            return this;
        }

        public Builder source(String name, String content) {
            delegate.source(name, content);
            return this;
        }

        public Builder template(String template) {
            delegate.template(template);
            return this;
        }

        @Deprecated
        public Builder flow(String flow) {
            delegate.flow(flow);
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            delegate.dependencies(dependencies);
            return this;
        }

        public Builder dataTypes(Map<String, KameletSpec.DataTypesSpec> types) {
            delegate.dataTypes(types);
            return this;
        }

        public Builder addDataType(String slot, String scheme, String format) {
            delegate.addDataType(slot, scheme, format);
            return this;
        }

        public Builder from(Kamelet kamelet) {
            delegate.name(kamelet.getMetadata().getName());

            if (kamelet.getSpec() != null) {
                if (kamelet.getSpec().getDefinition() != null) {
                    delegate.definition(kamelet.getSpec().getDefinition());
                }

                if (kamelet.getSpec().getSources() != null && !kamelet.getSpec().getSources().isEmpty()) {
                    KameletSpec.Source source = kamelet.getSpec().getSources().get(0);
                    delegate.source(source.getName(), source.getContent());
                }

                if (kamelet.getSpec().getDependencies() != null) {
                    delegate.dependencies(kamelet.getSpec().getDependencies());
                }

                if (kamelet.getSpec().getDataTypes() != null) {
                    delegate.dataTypes(kamelet.getSpec().getDataTypes());
                }
            }

            return this;
        }

        public KameletV1Alpha1 build() {
            Kamelet k = delegate.build();

            KameletV1Alpha1 kamelet = new KameletV1Alpha1();
            kamelet.setMetadata(k.getMetadata());

            kamelet.setSpec(k.getSpec());
            kamelet.setStatus(k.getStatus());

            return kamelet;
        }
    }
}
