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
import org.citrusframework.yaks.camelk.model.Pipe;
import org.citrusframework.yaks.camelk.model.PipeList;
import org.citrusframework.yaks.camelk.model.PipeSpec;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBinding;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBindingList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action creates new Camel K pipe with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type pipe.
 *
 * @author Christoph Deppisch
 */
public class CreatePipeAction extends AbstractKameletAction {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CreatePipeAction.class);

    private final String pipeName;
    private final Integration integration;
    private final PipeSpec.Endpoint source;
    private final PipeSpec.Endpoint sink;
    private final Resource resource;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreatePipeAction(Builder builder) {
        super("create-pipe", builder);
        this.pipeName = builder.pipeName;
        this.integration = builder.integration;
        this.source = builder.source;
        this.sink = builder.sink;
        this.resource = builder.resource;
    }

    @Override
    public void doExecute(TestContext context) {
        final Pipe pipe;

        String pipeName = context.replaceDynamicContentInString(this.pipeName);
        LOG.info(String.format("Creating Camel K pipe '%s'", pipeName));

        if (resource != null) {
            try {
                if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
                    pipe = KubernetesSupport.yaml().loadAs(
                            context.replaceDynamicContentInString(FileUtils.readToString(resource)), KameletBinding.class);
                } else {
                    pipe = KubernetesSupport.yaml().loadAs(
                            context.replaceDynamicContentInString(FileUtils.readToString(resource)), Pipe.class);
                }
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load pipe from resource %s", pipeName + ".yaml"), e);
            }
        } else {
            final Pipe.Builder builder = new Pipe.Builder()
                    .name(pipeName);

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

            pipe = builder.build();
        }

        if (YaksSettings.isLocal(clusterType(context))) {
            createLocalPipe(pipe, pipeName, context);
        } else {
            createPipe(getKubernetesClient(), namespace(context), pipe, context);
        }

        LOG.info(String.format("Successfully created pipe '%s'", pipe.getMetadata().getName()));
    }

    /**
     * Creates the pipe as a custom resource in given namespace.
     * @param k8sClient
     * @param namespace
     * @param pipe
     * @param context
     */
    private void createPipe(KubernetesClient k8sClient, String namespace, Pipe pipe, TestContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml().dumpAsMap(pipe));
        }

        if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
            KameletBinding kb;
            if (pipe instanceof KameletBinding) {
                kb = (KameletBinding) pipe;
            } else {
                kb = new KameletBinding.Builder().from(pipe).build();
            }

            k8sClient.resources(KameletBinding.class, KameletBindingList.class)
                    .inNamespace(namespace)
                    .resource(kb)
                    .createOrReplace();
        } else {
            k8sClient.resources(Pipe.class, PipeList.class)
                    .inNamespace(namespace)
                    .resource(pipe)
                    .createOrReplace();
        }
    }

    /**
     * Creates the pipe with local JBang runtime.
     * @param pipe
     * @param name
     * @param context
     */
    private void createLocalPipe(Pipe pipe, String name, TestContext context) {
        try {
            String pipeYaml;

            if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
                KameletBinding kb;
                if (pipe instanceof KameletBinding) {
                    kb = (KameletBinding) pipe;
                } else {
                    kb = new KameletBinding.Builder().from(pipe).build();
                }

                pipeYaml = KubernetesSupport.yaml().dumpAsMap(kb);
            } else {
                pipeYaml = KubernetesSupport.yaml().dumpAsMap(pipe);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(pipeYaml);
            }

            Path workDir = CamelJBangSettings.getWorkDir();
            Files.createDirectories(workDir);
            Path file = workDir.resolve(String.format("i-%s.yaml", name));
            Files.write(file, pipeYaml.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            ProcessAndOutput pao = camel().run(name, file);

            if (!pao.getProcess().isAlive()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(pao.getOutput());
                }

                throw new CitrusRuntimeException(String.format("Failed to create pipe - exit code %s", pao.getProcess().exitValue()));
            }

            Long pid = pao.getCamelProcessId();
            context.setVariable(name + ":pid", pid);
            context.setVariable(name + ":process:" + pid, pao);
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to create pipe file", e);
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<CreatePipeAction, Builder> {

        private String pipeName;
        private Integration integration;
        private PipeSpec.Endpoint source;
        private PipeSpec.Endpoint sink;
        private Resource resource;

        public Builder binding(String pipeName) {
            apiVersion(CamelKSettings.V1ALPHA1);
            this.pipeName = pipeName;
            return this;
        }

        public Builder pipe(String pipeName) {
            this.pipeName = pipeName;
            return this;
        }

        public Builder integration(Integration integration) {
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

        public Builder fromBuilder(Pipe.Builder builder) {
            Pipe pipe = builder.build();

            pipeName = pipe.getMetadata().getName();
            integration = pipe.getSpec().getIntegration();
            source = pipe.getSpec().getSource();
            sink = pipe.getSpec().getSink();

            return this;
        }

        public Builder resource(Resource resource) {
            this.resource = resource;
            return this;
        }

        @Override
        public CreatePipeAction build() {
            return new CreatePipeAction(this);
        }
    }
}
