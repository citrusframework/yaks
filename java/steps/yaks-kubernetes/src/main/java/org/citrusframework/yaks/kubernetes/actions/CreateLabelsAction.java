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

package org.citrusframework.yaks.kubernetes.actions;

import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;

/**
 * @author Christoph Deppisch
 */
public class CreateLabelsAction extends AbstractKubernetesAction implements KubernetesAction {

    private final String resourceName;
    private final ResourceType resourceType;
    private final Map<String, String> labels;

    public CreateLabelsAction(Builder builder) {
        super("create-label", builder);

        this.resourceName = builder.resourceName;
        this.resourceType = builder.resourceType;
        this.labels = builder.labels;
    }

    /**
     * Enumeration of supported Kubernetes resources this action is capable of adding labels to.
     */
    public enum ResourceType {
        POD,
        SECRET,
        SERVICE
    }

    @Override
    public void doExecute(TestContext context) {
        Map<String, String> resolvedLabels = context.resolveDynamicValuesInMap(labels);

        switch (resourceType) {
            case POD:
                getKubernetesClient().pods()
                        .inNamespace(namespace(context))
                        .withName(resourceName)
                        .edit(p -> new PodBuilder(p)
                                    .editMetadata()
                                        .addToLabels(resolvedLabels)
                                    .endMetadata()
                                .build());
                break;
            case SERVICE:
                getKubernetesClient().services()
                        .inNamespace(namespace(context))
                        .withName(resourceName)
                        .edit(s -> new ServiceBuilder(s)
                                    .editMetadata()
                                        .addToLabels(resolvedLabels)
                                    .endMetadata()
                                .build());
                break;
            case SECRET:
                getKubernetesClient().secrets()
                        .inNamespace(namespace(context))
                        .withName(resourceName)
                        .edit(s -> new SecretBuilder(s)
                                    .editMetadata()
                                        .addToLabels(resolvedLabels)
                                    .endMetadata()
                                .build());
                break;
            default:
                throw new CitrusRuntimeException(String.format("Unable to add label to resource type '%s'", resourceType.name()));
        }
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKubernetesAction.Builder<CreateLabelsAction, Builder> {

        private String resourceName;
        private ResourceType resourceType = ResourceType.POD;
        private final Map<String, String> labels = new HashMap<>();

        public Builder name(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder pod(String name) {
            this.resourceName = name;
            return type(ResourceType.POD);
        }

        public Builder secret(String name) {
            this.resourceName = name;
            return type(ResourceType.SECRET);
        }

        public Builder service(String name) {
            this.resourceName = name;
            return type(ResourceType.SERVICE);
        }

        private Builder type(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder type(String resourceType) {
            return type(ResourceType.valueOf(resourceType));
        }

        public Builder labels(Map<String, String>labels) {
            this.labels.putAll(labels);
            return this;
        }

        public Builder label(String label, String value) {
            this.labels.put(label, value);
            return this;
        }

        @Override
        public CreateLabelsAction build() {
            return new CreateLabelsAction(this);
        }
    }
}
