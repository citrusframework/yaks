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

package org.citrusframework.yaks.camelk.actions.kamelet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.client.dsl.Updatable;
import org.apache.camel.v1.Kamelet;
import org.apache.camel.v1.KameletBuilder;
import org.apache.camel.v1.KameletSpecBuilder;
import org.apache.camel.v1.kameletspec.DataTypes;
import org.apache.camel.v1.kameletspec.DataTypesBuilder;
import org.apache.camel.v1.kameletspec.Definition;
import org.apache.camel.v1.kameletspec.Sources;
import org.apache.camel.v1.kameletspec.SourcesBuilder;
import org.apache.camel.v1.kameletspec.TemplateBuilder;
import org.apache.camel.v1.kameletspec.datatypes.Types;
import org.apache.camel.v1.kameletspec.datatypes.TypesBuilder;
import org.apache.camel.v1.kameletspec.definition.Properties;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.spi.Resource;
import org.citrusframework.util.FileUtils;
import org.citrusframework.util.IsJsonPredicate;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.KameletSettings;
import org.citrusframework.yaks.camelk.model.KameletList;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletV1Alpha1;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletV1Alpha1List;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.springframework.util.StringUtils;

/**
 * Test action creates new Camel K integration with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type integration.
 *
 * @author Christoph Deppisch
 */
public class CreateKameletAction extends AbstractKameletAction {

    private final String name;
    private final String template;
    private final Sources source;
    private final Definition definition;
    private final List<String> dependencies;
    private final Map<String, DataTypes> dataTypes;
    private final Resource resource;
    private final boolean supportVariables;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateKameletAction(Builder builder) {
        super("create-kamelet", builder);
        this.name = builder.name;
        this.template = builder.template;
        this.source = builder.source;
        this.definition = builder.definition;
        this.dependencies = builder.dependencies;
        this.dataTypes = builder.dataTypes;
        this.resource = builder.resource;
        this.supportVariables = builder.supportVariables;
    }

    @Override
    public void doExecute(TestContext context) {
        createKamelet(context);
    }

    private void createKamelet(TestContext context) {
        final Kamelet kamelet;

        if (resource != null) {
            try {
                String resolvedSource;
                if (supportVariables) {
                    resolvedSource = context.replaceDynamicContentInString(FileUtils.readToString(resource));
                } else {
                    resolvedSource = FileUtils.readToString(resource);
                }

                if (IsJsonPredicate.getInstance().test(resolvedSource)) {
                    kamelet = KubernetesSupport.json().readValue(resolvedSource, Kamelet.class);
                } else {
                    // need to make a detour over Json to support additional properties set on Pipe
                    Map<String, Object> raw = KubernetesSupport.yaml().load(resolvedSource);
                    kamelet = KubernetesSupport.json().convertValue(raw, Kamelet.class);
                }
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load Kamelet from resource %s", name + ".kamelet.yaml"), e);
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

            final KameletBuilder builder = new KameletBuilder()
                    .withNewMetadata()
                        .withName(context.replaceDynamicContentInString(name))
                    .endMetadata();

            KameletSpecBuilder specBuilder = new KameletSpecBuilder()
                    .withDefinition(definition);

            if (template != null) {
                specBuilder.withTemplate(new TemplateBuilder()
                            .withAdditionalProperties(KubernetesSupport.yaml().load(context.replaceDynamicContentInString(template)))
                        .build());
            }

            if (source != null) {
                specBuilder.withSources(new SourcesBuilder()
                            .withName(source.getName())
                            .withContent(context.replaceDynamicContentInString(source.getContent()))
                        .build());
            }

            if (dependencies != null && !dependencies.isEmpty()) {
                specBuilder.withDependencies(context.resolveDynamicValuesInList(dependencies));
            }

            if (dataTypes != null && !dataTypes.isEmpty()) {
                specBuilder.withDataTypes(dataTypes);
            }

            kamelet = builder.withSpec(specBuilder.build()).build();
        }

        if (!kamelet.getMetadata().getLabels().containsKey(KameletSettings.KAMELET_TYPE_LABEL)) {
            if (kamelet.getMetadata().getName().endsWith("-source")) {
                kamelet.getMetadata().getLabels().put(KameletSettings.KAMELET_TYPE_LABEL, "source");
            } else if (kamelet.getMetadata().getName().endsWith("-sink")) {
                kamelet.getMetadata().getLabels().put(KameletSettings.KAMELET_TYPE_LABEL, "sink");
            } else if (kamelet.getMetadata().getName().endsWith("-action")) {
                kamelet.getMetadata().getLabels().put(KameletSettings.KAMELET_TYPE_LABEL, "action");
            } else {
                throw new CitrusRuntimeException(String.format("Unsupported Kamelet type - failed to determine type from Kamelet name %s, " +
                        "expected one of '-source', '-sink' or '-action' suffix", kamelet.getMetadata().getName()));
            }
        }

        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug(KubernetesSupport.json().writeValueAsString(kamelet));
            } catch (JsonProcessingException e) {
                LOG.warn("Unable to dump Kamelet data", e);
            }
        }

        if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
            KameletV1Alpha1 kameletV1Alpha1 = KameletV1Alpha1.from(kamelet);

            getKubernetesClient().resources(KameletV1Alpha1.class, KameletV1Alpha1List.class)
                    .inNamespace(kameletNamespace(context))
                    .resource(kameletV1Alpha1)
                    .createOr(Updatable::update);
        } else {
            getKubernetesClient().resources(Kamelet.class, KameletList.class)
                    .inNamespace(kameletNamespace(context))
                    .resource(kamelet)
                    .createOr(Updatable::update);
        }

        LOG.info(String.format("Successfully created Kamelet '%s'", kamelet.getMetadata().getName()));
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<CreateKameletAction, Builder> {

        private String name;
        private String template;
        private Sources source;
        private final List<String> dependencies = new ArrayList<>();
        private Definition definition = new Definition();
        private final Map<String, DataTypes> dataTypes = new HashMap<>();

        private Resource resource;

        private boolean supportVariables = true;

        public Builder supportVariables(boolean supportVariables) {
            this.supportVariables = supportVariables;
            return this;
        }

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
            this.source = new SourcesBuilder()
                        .withName(name + "." + language)
                        .withContent(content)
                    .build();
            return this;
        }

        public Builder source(String name, String content) {
            this.source = new SourcesBuilder()
                        .withName(name)
                        .withContent(content)
                    .build();
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

        public Builder definition(Definition definition) {
            this.definition = definition;
            return this;
        }

        public Builder addProperty(String name, Properties propertyConfig) {
            this.definition.getProperties().put(name, propertyConfig);
            return this;
        }

        public Builder inType(String scheme, String format) {
            return addDataType("in", scheme, format);
        }

        public Builder outType(String scheme, String format) {
            return addDataType("out", scheme, format);
        }

        public Builder errorType(String scheme, String format) {
            return addDataType("error", scheme, format);
        }

        public Builder addDataType(String slot, String scheme, String format) {
            if (dataTypes.containsKey(slot)) {
                this.dataTypes.get(slot).getTypes().put(format, new TypesBuilder().withScheme(scheme).withFormat(format).build());
            } else {
                Map<String, Types> dataTypes = new HashMap<>();
                dataTypes.put(format, new TypesBuilder().withScheme(scheme).withFormat(format).build());
                this.dataTypes.put(slot, new DataTypesBuilder().withTypes(dataTypes).build());
            }

            return this;
        }

        public Builder fromBuilder(KameletBuilder builder) {
            Kamelet kamelet = builder.build();

            name = kamelet.getMetadata().getName();
            definition = kamelet.getSpec().getDefinition();

            if (kamelet.getSpec().getDependencies() != null) {
                dependencies.addAll(kamelet.getSpec().getDependencies());
            }

            if (kamelet.getSpec().getDataTypes() != null) {
                dataTypes.putAll(kamelet.getSpec().getDataTypes());
            }

            if (kamelet.getSpec().getSources() != null && !kamelet.getSpec().getSources().isEmpty()) {
                source = kamelet.getSpec().getSources().get(0);
            }

            if (kamelet.getSpec().getTemplate() != null) {
                template = KubernetesSupport.dumpYaml(kamelet.getSpec().getTemplate());
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
