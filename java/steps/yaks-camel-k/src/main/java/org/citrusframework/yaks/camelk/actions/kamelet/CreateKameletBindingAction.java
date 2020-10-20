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

package org.citrusframework.yaks.camelk.actions.kamelet;

import java.io.IOException;
import java.util.Map;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.DoneableKameletBinding;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.KameletBinding;
import org.citrusframework.yaks.camelk.model.KameletBindingList;
import org.citrusframework.yaks.camelk.model.KameletBindingSpec;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.core.io.Resource;

/**
 * Test action creates new Camel-K integration with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type integration.
 *
 * @author Christoph Deppisch
 */
public class CreateKameletBindingAction extends AbstractCamelKAction {

    private final String name;
    private final Integration integration;
    private final KameletBindingSpec.Endpoint source;
    private final KameletBindingSpec.Endpoint sink;
    private final Resource resource;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateKameletBindingAction(Builder builder) {
        super("create-kamelet-binding", builder);
        this.name = builder.name;
        this.integration = builder.integration;
        this.source = builder.source;
        this.sink = builder.sink;
        this.resource = builder.resource;
    }

    @Override
    public void doExecute(TestContext context) {
        createKameletBinding(context);
    }

    private void createKameletBinding(TestContext context) {
        final KameletBinding binding;

        if (resource != null) {
            try {
                binding = KubernetesSupport.yaml().loadAs(
                        context.replaceDynamicContentInString(FileUtils.readToString(resource)), KameletBinding.class);
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load KameletBinding from resource %s", name + ".kamelet.yaml"));
            }
        } else {
            final KameletBinding.Builder builder = new KameletBinding.Builder()
                    .name(context.replaceDynamicContentInString(name));

            if (integration != null) {
                builder.integration(integration);
            }

            if (source != null) {
                builder.source(source);
            }

            if (sink != null) {
                if (sink.getUri() != null) {
                    sink.setUri(context.replaceDynamicContentInString(sink.getUri()));
                }

                builder.sink(sink);
            }

            binding = builder.build();
        }

        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug(KubernetesSupport.json().writeValueAsString(binding));
            } catch (JsonProcessingException e) {
                LOG.warn("Unable to dump KameletBinding data", e);
            }
        }

        CustomResourceDefinitionContext ctx = CamelKSupport.kameletBindingCRDContext(CamelKSettings.getKameletApiVersion());
        getKubernetesClient().customResources(ctx, KameletBinding.class, KameletBindingList.class, DoneableKameletBinding.class)
                .inNamespace(CamelKSettings.getNamespace())
                .createOrReplace(binding);

        LOG.info(String.format("Successfully created KameletBinding '%s'", binding.getMetadata().getName()));
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<CreateKameletBindingAction, Builder> {

        private String name;
        private Integration integration;
        private KameletBindingSpec.Endpoint source;
        private KameletBindingSpec.Endpoint sink;
        private Resource resource;

        public Builder binding(String kameletName) {
            this.name = kameletName;
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

        public Builder fromBuilder(KameletBinding.Builder builder) {
            KameletBinding binding = builder.build();

            name = binding.getMetadata().getName();
            integration = binding.getSpec().getIntegration();
            source = binding.getSpec().getSource();
            sink = binding.getSpec().getSink();

            return this;
        }

        public Builder resource(Resource resource) {
            this.resource = resource;
            return this;
        }

        @Override
        public CreateKameletBindingAction build() {
            return new CreateKameletBindingAction(this);
        }
    }
}
