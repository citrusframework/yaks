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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletList;
import org.citrusframework.yaks.camelk.model.KameletSpec;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Test action creates new Camel-K integration with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type integration.
 *
 * @author Christoph Deppisch
 */
public class CreateKameletAction extends AbstractCamelKAction {

    private final String name;
    private final String flow;
    private final KameletSpec.Source source;
    private final KameletSpec.Definition definition;
    private final List<String> dependencies;
    private final Map<String, KameletSpec.TypeSpec> types;
    private final Resource resource;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateKameletAction(Builder builder) {
        super("create-kamelet", builder);
        this.name = builder.name;
        this.flow = builder.flow;
        this.source = builder.source;
        this.definition = builder.definition;
        this.dependencies = builder.dependencies;
        this.types = builder.types;
        this.resource = builder.resource;
    }

    @Override
    public void doExecute(TestContext context) {
        createKamelet(context);
    }

    private void createKamelet(TestContext context) {
        final Kamelet kamelet;

        if (resource != null) {
            try {
                kamelet = KubernetesSupport.yaml().loadAs(
                        context.replaceDynamicContentInString(FileUtils.readToString(resource)), Kamelet.class);
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load Kamelet from resource %s", name + ".kamelet.yaml"));
            }
        } else {
            if (definition.getTitle() != null) {
                definition.setTitle(context.replaceDynamicContentInString(definition.getTitle()));
            }

            if (definition.getDescription() != null) {
                definition.setDescription(context.replaceDynamicContentInString(definition.getDescription()));
            }

            definition.setProperties(context.resolveDynamicValuesInMap(definition.getProperties()));
            definition.setRequired(context.resolveDynamicValuesInList(definition.getRequired()));

            final Kamelet.Builder builder = new Kamelet.Builder()
                    .name(context.replaceDynamicContentInString(name))
                    .definition(definition);

            if (flow != null) {
                builder.flow(context.replaceDynamicContentInString(flow));
            }

            if (source != null) {
                builder.source(source.getName(), context.replaceDynamicContentInString(source.getContent()));
            }

            if (dependencies != null && !dependencies.isEmpty()) {
                builder.dependencies(context.resolveDynamicValuesInList(dependencies));
            }

            if (types != null && !types.isEmpty()) {
                builder.types(context.resolveDynamicValuesInMap(types));
            }

            kamelet = builder.build();
        }

        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug(KubernetesSupport.json().writeValueAsString(kamelet));
            } catch (JsonProcessingException e) {
                LOG.warn("Unable to dump Kamelet data", e);
            }
        }

        CustomResourceDefinitionContext ctx = CamelKSupport.kameletCRDContext(CamelKSettings.getKameletApiVersion());
        getKubernetesClient().customResources(ctx, Kamelet.class, KameletList.class)
                .inNamespace(CamelKSettings.getNamespace())
                .createOrReplace(kamelet);

        LOG.info(String.format("Successfully created Kamelet '%s'", kamelet.getMetadata().getName()));
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<CreateKameletAction, Builder> {

        private String name;
        private String flow;
        private KameletSpec.Source source;
        private List<String> dependencies = new ArrayList<>();
        private KameletSpec.Definition definition = new KameletSpec.Definition();
        private Map<String, KameletSpec.TypeSpec> types = new HashMap<>();
        private Resource resource;

        public Builder kamelet(String kameletName) {
            this.name = kameletName;
            title(StringUtils.capitalize(kameletName));
            return this;
        }

        public Builder title(String title) {
            this.definition.setTitle(title);
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

        public Builder dependencies(String dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies.split(",")));
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public Builder dependency(String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder definition(KameletSpec.Definition definition) {
            this.definition = definition;
            return this;
        }

        public Builder addProperty(String name, KameletSpec.Definition.PropertyConfig propertyConfig) {
            this.definition.getProperties().put(name, propertyConfig);
            return this;
        }

        public Builder inType(String mediaType) {
            return addType("in", mediaType);
        }

        public Builder outType(String mediaType) {
            return addType("out", mediaType);
        }

        public Builder errorType(String mediaType) {
            return addType("error", mediaType);
        }

        public Builder addType(String slot, String mediaType) {
            this.types.put(slot, new KameletSpec.TypeSpec(mediaType));
            return this;
        }

        public Builder fromBuilder(Kamelet.Builder builder) {
            Kamelet kamelet = builder.build();

            name = kamelet.getMetadata().getName();
            definition = kamelet.getSpec().getDefinition();
            dependencies.addAll(kamelet.getSpec().getDependencies());
            types.putAll(kamelet.getSpec().getTypes());

            if (kamelet.getSpec().getSources() != null && !kamelet.getSpec().getSources().isEmpty()) {
                source = kamelet.getSpec().getSources().get(0);
            }

            if (kamelet.getSpec().getFlow() != null) {
                flow = KubernetesSupport.yaml().dump(kamelet.getSpec().getFlow());
            }

            return this;
        }

        public Builder resource(Resource resource) {
            this.resource = resource;
            return this;
        }

        @Override
        public CreateKameletAction build() {
            return new CreateKameletAction(this);
        }
    }
}
