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

import org.citrusframework.context.TestContext;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * @author Christoph Deppisch
 */
public class DeleteCustomResourceAction extends AbstractKubernetesAction {

    private final String resourceName;
    private final String type;
    private final String version;
    private final String kind;
    private final String group;

    public DeleteCustomResourceAction(Builder builder) {
        super("delete-custom-resource", builder);

        this.resourceName = builder.resourceName;
        this.type = builder.type;
        this.group = builder.group;
        this.version = builder.version;
        this.kind = builder.kind;
    }

    @Override
    public void doExecute(TestContext context) {
        String resolvedName = context.replaceDynamicContentInString(resourceName);
        getKubernetesClient().genericKubernetesResources(
                KubernetesSupport.crdContext(
                    context.replaceDynamicContentInString(type),
                    context.replaceDynamicContentInString(group),
                    context.replaceDynamicContentInString(kind),
                    context.replaceDynamicContentInString(version)))
                .inNamespace(namespace(context))
                .withName(resolvedName)
                .delete();
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKubernetesAction.Builder<DeleteCustomResourceAction, Builder> {

        private String resourceName;
        private String type;
        private String version;
        private String kind;
        private String group;

        public Builder resourceName(String name) {
            this.resourceName = name;
            return this;
        }

        public Builder type(String resourceType) {
            this.type = resourceType;
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            String[] groupAndVersion = apiVersion.split("/");

            group(groupAndVersion[0]);
            version(groupAndVersion[1]);
            return this;
        }

        @Override
        public DeleteCustomResourceAction build() {
            return new DeleteCustomResourceAction(this);
        }
    }
}
