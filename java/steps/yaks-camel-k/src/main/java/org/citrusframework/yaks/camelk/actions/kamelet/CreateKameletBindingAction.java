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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Updatable;
import org.apache.camel.v1alpha1.KameletBinding;
import org.apache.camel.v1alpha1.KameletBindingBuilder;
import org.apache.camel.v1alpha1.KameletBindingSpecBuilder;
import org.apache.camel.v1alpha1.kameletbindingspec.Integration;
import org.apache.camel.v1alpha1.kameletbindingspec.Sink;
import org.apache.camel.v1alpha1.kameletbindingspec.SinkBuilder;
import org.apache.camel.v1alpha1.kameletbindingspec.Source;
import org.apache.camel.v1alpha1.kameletbindingspec.SourceBuilder;
import org.apache.camel.v1alpha1.kameletbindingspec.source.Ref;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.spi.Resource;
import org.citrusframework.util.FileUtils;
import org.citrusframework.util.IsJsonPredicate;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.jbang.CamelJBangSettings;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBindingList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action creates new Camel K binding with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type binding.
 *
 * @author Christoph Deppisch
 */
public class CreateKameletBindingAction extends AbstractKameletAction {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CreateKameletBindingAction.class);

    private final String bindingName;
    private final Integration integration;
    private final Source source;
    private final Sink sink;
    private final Resource resource;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateKameletBindingAction(Builder builder) {
        super("create-binding", builder);
        this.bindingName = builder.bindingName;
        this.integration = builder.integration;
        this.source = builder.source;
        this.sink = builder.sink;
        this.resource = builder.resource;
    }

    @Override
    public void doExecute(TestContext context) {
        final KameletBinding binding;

        String bindingName = context.replaceDynamicContentInString(this.bindingName);
        LOG.info(String.format("Creating Camel K binding '%s'", bindingName));

        if (resource != null) {
            try {
                String yamlOrJson = context.replaceDynamicContentInString(FileUtils.readToString(resource));
                if (IsJsonPredicate.getInstance().test(yamlOrJson)) {
                    binding = KubernetesSupport.json().readValue(yamlOrJson, KameletBinding.class);
                } else {
                    // need to make a detour over Json to support additional properties set on Pipe
                    Map<String, Object> raw = KubernetesSupport.yaml().load(yamlOrJson);
                    binding = KubernetesSupport.json().convertValue(raw, KameletBinding.class);
                }
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load binding from resource %s", bindingName + ".yaml"), e);
            }
        } else {
            final KameletBindingBuilder builder = new KameletBindingBuilder()
                    .withNewMetadata()
                    .withName(bindingName)
                    .endMetadata();

            KameletBindingSpecBuilder specBuilder = new KameletBindingSpecBuilder();
            if (integration != null) {
                specBuilder.withIntegration(integration);
            }

            if (source != null) {
                if (source.getProperties() != null && source.getProperties().getAdditionalProperties() != null) {
                    context.resolveDynamicValuesInMap(source.getProperties().getAdditionalProperties());
                }
                specBuilder.withSource(source);
            }

            if (sink != null) {
                if (sink.getUri() != null) {
                    sink.setUri(context.replaceDynamicContentInString(sink.getUri()));
                }

                if (sink.getProperties() != null && sink.getProperties().getAdditionalProperties() != null) {
                    context.resolveDynamicValuesInMap(sink.getProperties().getAdditionalProperties());
                }
                specBuilder.withSink(sink);
            }

            binding = builder.withSpec(specBuilder.build()).build();
        }

        if (YaksSettings.isLocal(clusterType(context))) {
            createLocal(KubernetesSupport.yaml(new KameletBindingValuePropertyMapper()).dumpAsMap(binding), bindingName, context);
        } else {
            createKameletBinding(getKubernetesClient(), namespace(context), binding, context);
        }

        LOG.info(String.format("Successfully created binding '%s'", binding.getMetadata().getName()));
    }

    /**
     * Creates the Kamelet binding as a custom resource in given namespace.
     * @param k8sClient
     * @param namespace
     * @param binding
     * @param context
     */
    private void createKameletBinding(KubernetesClient k8sClient, String namespace, KameletBinding binding, TestContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml(new KameletBindingValuePropertyMapper()).dumpAsMap(binding));
        }

        k8sClient.resources(KameletBinding.class, KameletBindingList.class)
                .inNamespace(namespace)
                .resource(binding)
                .createOr(Updatable::update);
    }

    /**
     * Creates the binding with local JBang runtime.
     * @param yaml
     * @param name
     * @param context
     */
    private void createLocal(String yaml, String name, TestContext context) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(yaml);
            }

            Path workDir = CamelJBangSettings.getWorkDir();
            Files.createDirectories(workDir);
            Path file = workDir.resolve(String.format("i-%s.yaml", name));
            Files.writeString(file, yaml,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            ProcessAndOutput pao = camel().run(name, file);

            if (!pao.getProcess().isAlive()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(pao.getOutput());
                }

                throw new CitrusRuntimeException(String.format("Failed to create binding - exit code %s", pao.getProcess().exitValue()));
            }

            Long pid = pao.getCamelProcessId();
            context.setVariable(name + ":pid", pid);
            context.setVariable(name + ":process:" + pid, pao);
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to create binding file", e);
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<CreateKameletBindingAction, Builder> {

        private String bindingName;
        private Integration integration;
        private Source source;
        private Sink sink;
        private Resource resource;

        public Builder binding(String bindingName) {
            apiVersion(CamelKSettings.V1ALPHA1);
            this.bindingName = bindingName;
            return this;
        }

        public Builder integration(Integration integration) {
            this.integration = integration;
            return this;
        }

        public Builder source(Source source) {
            this.source = source;
            return this;
        }

        public Builder source(String uri) {
            return source(new SourceBuilder().withUri(uri).build());
        }

        public Builder source(Ref ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return source(new SourceBuilder().withRef(ref)
                    .withNewProperties()
                        .addToAdditionalProperties(props)
                    .endProperties().build());
        }

        public Builder sink(Sink sink) {
            this.sink = sink;
            return this;
        }

        public Builder sink(String uri) {
            return sink(new SinkBuilder().withUri(uri).build());
        }

        public Builder sink(org.apache.camel.v1alpha1.kameletbindingspec.sink.Ref ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return sink(new SinkBuilder().withRef(ref)
                    .withNewProperties()
                        .addToAdditionalProperties(props)
                    .endProperties()
                    .build());
        }

        public Builder fromBuilder(KameletBindingBuilder builder) {
            KameletBinding binding = builder.build();

            bindingName = binding.getMetadata().getName();
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
