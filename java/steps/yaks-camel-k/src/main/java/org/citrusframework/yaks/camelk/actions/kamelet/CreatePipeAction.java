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
import org.apache.camel.v1.Pipe;
import org.apache.camel.v1.PipeBuilder;
import org.apache.camel.v1.PipeSpecBuilder;
import org.apache.camel.v1.pipespec.Integration;
import org.apache.camel.v1.pipespec.Sink;
import org.apache.camel.v1.pipespec.SinkBuilder;
import org.apache.camel.v1.pipespec.Source;
import org.apache.camel.v1.pipespec.SourceBuilder;
import org.apache.camel.v1.pipespec.source.Ref;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.spi.Resource;
import org.citrusframework.util.FileUtils;
import org.citrusframework.util.IsJsonPredicate;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.jbang.CamelJBangSettings;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.citrusframework.yaks.camelk.model.PipeList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Source source;
    private final Sink sink;
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
                String yamlOrJson = context.replaceDynamicContentInString(FileUtils.readToString(resource));
                if (IsJsonPredicate.getInstance().test(yamlOrJson)) {
                    pipe = KubernetesSupport.json().readValue(yamlOrJson, Pipe.class);
                } else {
                    // need to make a detour over Json to support additional properties set on Pipe
                    Map<String, Object> raw = KubernetesSupport.yaml().load(yamlOrJson);
                    pipe = KubernetesSupport.json().convertValue(raw, Pipe.class);
                }
            } catch (IOException e) {
                throw new CitrusRuntimeException(String.format("Failed to load pipe from resource %s", pipeName + ".yaml"), e);
            }
        } else {
            final PipeBuilder builder = new PipeBuilder()
                    .withNewMetadata()
                    .withName(pipeName)
                    .endMetadata();

            PipeSpecBuilder specBuilder = new PipeSpecBuilder();
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

            pipe = builder.withSpec(specBuilder.build()).build();
        }

        if (YaksSettings.isLocal(clusterType(context))) {
            createLocal(KubernetesSupport.dumpYaml(pipe), pipeName, context);
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
            LOG.debug(KubernetesSupport.dumpYaml(pipe));
        }

        k8sClient.resources(Pipe.class, PipeList.class)
                .inNamespace(namespace)
                .resource(pipe)
                .createOr(Updatable::update);
    }

    /**
     * Creates the pipe with local JBang runtime.
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
        private Source source;
        private Sink sink;
        private Resource resource;

        public Builder pipe(String pipeName) {
            this.pipeName = pipeName;
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

        public Builder sink(org.apache.camel.v1.pipespec.sink.Ref ref, String properties) {
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

        public Builder fromBuilder(PipeBuilder builder) {
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
