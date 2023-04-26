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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.Pipe;
import org.citrusframework.yaks.camelk.model.PipeSpec;

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(CamelKSupport.CAMELK_CRD_GROUP)
@Version(CamelKSettings.V1ALPHA1)
public class KameletBinding extends Pipe {

    /**
     * Fluent builder
     */
    public static class Builder extends Pipe.Builder {

        Pipe.Builder delegate = new Pipe.Builder();

        public Builder name(String name) {
            this.name = name;
            delegate.name(name);
            return this;
        }

        public Builder integration(Integration integration) {
            delegate.integration(integration);
            return this;
        }

        public Builder source(PipeSpec.Endpoint source) {
            delegate.source(source);
            return this;
        }

        public Builder source(String uri) {
            delegate.source(uri);
            return this;
        }

        public Builder source(PipeSpec.Endpoint.ObjectReference ref, String properties) {
            delegate.source(ref, properties);
            return this;
        }

        public Builder sink(PipeSpec.Endpoint sink) {
            delegate.sink(sink);
            return this;
        }

        public Builder sink(String uri) {
            delegate.sink(uri);
            return this;
        }

        public Builder sink(PipeSpec.Endpoint.ObjectReference ref, String properties) {
            delegate.sink(ref, properties);
            return this;
        }

        public Builder steps(PipeSpec.Endpoint... step) {
            delegate.steps(step);
            return this;
        }

        public Builder addStep(PipeSpec.Endpoint step) {
            delegate.addStep(step);
            return this;
        }

        public Builder addStep(String uri) {
            delegate.addStep(new PipeSpec.Endpoint(uri));
            return this;
        }

        public Builder addStep(PipeSpec.Endpoint.ObjectReference ref, String properties) {
            delegate.addStep(ref, properties);
            return this;
        }

        public Builder replicas(int replicas) {
            delegate.replicas(replicas);
            return this;
        }

        public Builder from(Pipe pipe) {
            delegate.name(pipe.getMetadata().getName());
            delegate.source(pipe.getSpec().getSource());
            delegate.sink(pipe.getSpec().getSink());

            if (pipe.getSpec().getSteps() != null) {
                delegate.steps(pipe.getSpec().getSteps());
            }

            delegate.integration(pipe.getSpec().getIntegration());

            if (pipe.getSpec().getReplicas() != null) {
                delegate.replicas(pipe.getSpec().getReplicas());
            }

            return this;
        }

        public KameletBinding build() {
            Pipe b = this.delegate.build();

            KameletBinding binding = new KameletBinding();
            binding.setMetadata(b.getMetadata());

            binding.setSpec(b.getSpec());
            binding.setStatus(b.getStatus());

            return binding;
        }
    }
}
