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

import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * @author Christoph Deppisch
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(CamelKSupport.CAMELK_CRD_GROUP)
@Version(CamelKSettings.V1)
public class Pipe extends CustomResource<PipeSpec, PipeStatus> implements Namespaced {

    public Pipe() {
        super();
        this.spec = new PipeSpec();
        this.status = null;
    }

    /**
     * Fluent builder
     */
    public static class Builder {
        protected String name;
        private int replicas;
        private IntegrationSpec integration;
        private PipeSpec.Endpoint source;
        private PipeSpec.Endpoint sink;
        private final List<PipeSpec.Endpoint> steps = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder integration(IntegrationSpec integration) {
            this.integration = integration;
            return this;
        }

        public Builder source(PipeSpec.Endpoint source) {
            this.source = source;
            return this;
        }

        public Builder source(String uri) {
            return source(new PipeSpec.Endpoint(uri));
        }

        public Builder source(PipeSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return source(new PipeSpec.Endpoint(ref, props));
        }

        public Builder sink(PipeSpec.Endpoint sink) {
            this.sink = sink;
            return this;
        }

        public Builder sink(String uri) {
            return sink(new PipeSpec.Endpoint(uri));
        }

        public Builder sink(PipeSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return sink(new PipeSpec.Endpoint(ref, props));
        }

        public Builder steps(PipeSpec.Endpoint... step) {
            this.steps.addAll(Arrays.asList(step));
            return this;
        }

        public Builder addStep(PipeSpec.Endpoint step) {
            this.steps.add(step);
            return this;
        }

        public Builder addStep(String uri) {
            return addStep(new PipeSpec.Endpoint(uri));
        }

        public Builder addStep(PipeSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return addStep(new PipeSpec.Endpoint(ref, props));
        }

        public Builder replicas(int replicas) {
            this.replicas = replicas;
            return this;
        }

        public Pipe build() {
            Pipe pipe = new Pipe();
            pipe.getMetadata().setName(name);

            if (replicas > 0) {
                pipe.getSpec().setReplicas(replicas);
            }

            if (integration != null) {
                pipe.getSpec().setIntegration(integration);
            }

            if (source != null) {
                pipe.getSpec().setSource(source);
            }

            if (sink != null) {
                pipe.getSpec().setSink(sink);
            }

            if (!steps.isEmpty()) {
                pipe.getSpec().setSteps(steps.toArray(new PipeSpec.Endpoint[]{}));
            }

            return pipe;
        }
    }
}
