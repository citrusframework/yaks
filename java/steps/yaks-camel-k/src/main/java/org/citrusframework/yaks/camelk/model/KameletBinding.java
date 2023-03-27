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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(CamelKSupport.CAMELK_CRD_GROUP)
@Version(CamelKSettings.KAMELET_API_VERSION_DEFAULT)
public class KameletBinding extends Binding {

    /**
     * Fluent builder
     */
    public static class Builder extends Binding.Builder {

        Binding.Builder delegate = new Binding.Builder();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder integration(Integration integration) {
            delegate.integration(integration);
            return this;
        }

        public Builder source(BindingSpec.Endpoint source) {
            delegate.source(source);
            return this;
        }

        public Builder source(String uri) {
            delegate.source(uri);
            return this;
        }

        public Builder source(BindingSpec.Endpoint.ObjectReference ref, String properties) {
            delegate.source(ref, properties);
            return this;
        }

        public Builder sink(BindingSpec.Endpoint sink) {
            delegate.sink(sink);
            return this;
        }

        public Builder sink(String uri) {
            delegate.sink(uri);
            return this;
        }

        public Builder sink(BindingSpec.Endpoint.ObjectReference ref, String properties) {
            delegate.sink(ref, properties);
            return this;
        }

        public Builder steps(BindingSpec.Endpoint... step) {
            delegate.steps(step);
            return this;
        }

        public Builder addStep(BindingSpec.Endpoint step) {
            delegate.addStep(step);
            return this;
        }

        public Builder addStep(String uri) {
            delegate.addStep(new BindingSpec.Endpoint(uri));
            return this;
        }

        public Builder addStep(BindingSpec.Endpoint.ObjectReference ref, String properties) {
            delegate.addStep(ref, properties);
            return this;
        }

        public Builder replicas(int replicas) {
            delegate.replicas(replicas);
            return this;
        }

        public Builder from(Binding binding) {
            delegate.name(binding.getMetadata().getName());
            delegate.source(binding.getSpec().getSource());
            delegate.sink(binding.getSpec().getSink());

            if (binding.getSpec().getSteps() != null) {
                delegate.steps(binding.getSpec().getSteps());
            }

            delegate.integration(binding.getSpec().getIntegration());

            if (binding.getSpec().getReplicas() != null) {
                delegate.replicas(binding.getSpec().getReplicas());
            }

            return this;
        }

        public KameletBinding build() {
            Binding b = this.delegate.build();

            KameletBinding binding = new KameletBinding();
            binding.setMetadata(b.getMetadata());

            binding.setSpec(b.getSpec());
            binding.setStatus(b.getStatus());

            return binding;
        }
    }
}
