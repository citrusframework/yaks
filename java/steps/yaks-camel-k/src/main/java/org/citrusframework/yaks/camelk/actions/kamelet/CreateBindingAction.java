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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.jbang.CamelJBangSettings;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.citrusframework.yaks.camelk.model.Binding;
import org.citrusframework.yaks.camelk.model.BindingList;
import org.citrusframework.yaks.camelk.model.BindingSpec;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.KameletBinding;
import org.citrusframework.yaks.camelk.model.KameletBindingList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action creates new Camel K binding with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type binding.
 *
 * @author Christoph Deppisch
 */
public class CreateBindingAction extends AbstractKameletAction {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CreateBindingAction.class);

    private final String bindingName;
    private final Integration integration;
    private final BindingSpec.Endpoint source;
    private final BindingSpec.Endpoint sink;
    private final Resource resource;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateBindingAction(Builder builder) {
        super("create-binding", builder);
        this.bindingName = builder.bindingName;
        this.integration = builder.integration;
        this.source = builder.source;
        this.sink = builder.sink;
        this.resource = builder.resource;
    }

    @Override
    public void doExecute(TestContext context) {
        final Binding binding;

        String bindingName = context.replaceDynamicContentInString(this.bindingName);
        LOG.info(String.format("Creating Camel K binding '%s'", bindingName));

        if (resource != null) {
            try {
                if (apiVersion.equals(CamelKSettings.V1ALPHA1)) {
                    binding = KubernetesSupport.yaml().loadAs(
                            context.replaceDynamicContentInString(FileUtils.readToString(resource)), KameletBinding.class);
                } else {
                    binding = KubernetesSupport.yaml().loadAs(
                            context.replaceDynamicContentInString(FileUtils.readToString(resource)), Binding.class);
                }
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load binding from resource %s", bindingName + ".yaml"), e);
            }
        } else {
            final Binding.Builder builder = new Binding.Builder()
                    .name(bindingName);

            if (integration != null) {
                builder.integration(integration);
            }

            if (source != null) {
                source.setProperties(context.resolveDynamicValuesInMap(source.getProperties()));
                builder.source(source);
            }

            if (sink != null) {
                if (sink.getUri() != null) {
                    sink.setUri(context.replaceDynamicContentInString(sink.getUri()));
                }

                sink.setProperties(context.resolveDynamicValuesInMap(sink.getProperties()));
                builder.sink(sink);
            }

            binding = builder.build();
        }

        if (YaksSettings.isLocal(clusterType(context))) {
            createLocalBinding(binding, bindingName, context);
        } else {
            createBinding(getKubernetesClient(), namespace(context), binding);
        }

        LOG.info(String.format("Successfully created binding '%s'", binding.getMetadata().getName()));
    }

    /**
     * Creates the binding as a custom resource in given namespace.
     * @param k8sClient
     * @param namespace
     * @param binding
     */
    private void createBinding(KubernetesClient k8sClient, String namespace, Binding binding) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml().dumpAsMap(binding));
        }

        if (apiVersion.equals(CamelKSettings.V1ALPHA1)) {
            KameletBinding kb;
            if (binding instanceof KameletBinding) {
                kb = (KameletBinding) binding;
            } else {
                kb = new KameletBinding.Builder().from(binding).build();
            }

            k8sClient.resources(KameletBinding.class, KameletBindingList.class)
                    .inNamespace(namespace)
                    .resource(kb)
                    .createOrReplace();
        } else {
            k8sClient.resources(Binding.class, BindingList.class)
                    .inNamespace(namespace)
                    .resource(binding)
                    .createOrReplace();
        }
    }

    /**
     * Creates the binding with local JBang runtime.
     * @param binding
     * @param name
     * @param context
     */
    private void createLocalBinding(Binding binding, String name, TestContext context) {
        try {
            String bindingYaml = KubernetesSupport.yaml().dumpAsMap(binding);

            if (LOG.isDebugEnabled()) {
                LOG.debug(bindingYaml);
            }

            Path workDir = CamelJBangSettings.getWorkDir();
            Files.createDirectories(workDir);
            Path file = workDir.resolve(String.format("i-%s.yaml", name));
            Files.write(file, bindingYaml.getBytes(StandardCharsets.UTF_8),
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
    public static final class Builder extends AbstractKameletAction.Builder<CreateBindingAction, Builder> {

        private String bindingName;
        private Integration integration;
        private BindingSpec.Endpoint source;
        private BindingSpec.Endpoint sink;
        private Resource resource;

        public Builder binding(String bindingName) {
            this.bindingName = bindingName;
            return this;
        }

        public Builder integration(Integration integration) {
            this.integration = integration;
            return this;
        }

        public Builder source(BindingSpec.Endpoint source) {
            this.source = source;
            return this;
        }

        public Builder source(String uri) {
            return source(new BindingSpec.Endpoint(uri));
        }

        public Builder source(BindingSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return source(new BindingSpec.Endpoint(ref, props));
        }

        public Builder sink(BindingSpec.Endpoint sink) {
            this.sink = sink;
            return this;
        }

        public Builder sink(String uri) {
            return sink(new BindingSpec.Endpoint(uri));
        }

        public Builder sink(BindingSpec.Endpoint.ObjectReference ref, String properties) {
            Map<String, Object> props = null;
            if (properties != null && !properties.isEmpty()) {
                props = KubernetesSupport.yaml().load(properties);
            }

            return sink(new BindingSpec.Endpoint(ref, props));
        }

        public Builder fromBuilder(Binding.Builder builder) {
            Binding binding = builder.build();

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
        public CreateBindingAction build() {
            return new CreateBindingAction(this);
        }
    }
}
