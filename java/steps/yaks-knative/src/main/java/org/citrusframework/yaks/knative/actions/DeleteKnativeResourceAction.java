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

package org.citrusframework.yaks.knative.actions;

import com.consol.citrus.context.TestContext;
import org.citrusframework.yaks.knative.KnativeSupport;

/**
 * @author Christoph Deppisch
 */
public class DeleteKnativeResourceAction extends AbstractKnativeAction {

    private final String component;
    private final String kind;
    private final String resourceName;

    public DeleteKnativeResourceAction(Builder builder) {
        super("delete-" + builder.kind, builder);

        this.component = builder.component;
        this.kind = builder.kind;
        this.resourceName = builder.resourceName;
    }

    @Override
    public void doExecute(TestContext context) {
        KnativeSupport.deleteResource(getKubernetesClient(), namespace(context),
                KnativeSupport.knativeCRDContext(component, kind, KnativeSupport.knativeApiVersion()), resourceName);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKnativeAction.Builder<DeleteKnativeResourceAction, Builder> {

        private String component = "eventing";
        private String kind;
        private String resourceName;

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder name(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        @Override
        public DeleteKnativeResourceAction build() {
            return new DeleteKnativeResourceAction(this);
        }
    }
}
