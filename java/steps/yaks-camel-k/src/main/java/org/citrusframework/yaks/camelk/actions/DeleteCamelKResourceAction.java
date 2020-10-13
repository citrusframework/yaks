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

package org.citrusframework.yaks.camelk.actions;

import com.consol.citrus.context.TestContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;

/**
 * @author Christoph Deppisch
 */
public class DeleteCamelKResourceAction extends AbstractCamelKAction {

    private final String kind;
    private final String resourceName;

    public DeleteCamelKResourceAction(Builder builder) {
        super("delete-" + builder.kind, builder);

        this.kind = builder.kind;
        this.resourceName = builder.resourceName;
    }

    @Override
    public void doExecute(TestContext context) {
        CamelKSupport.deleteResource(getKubernetesClient(), CamelKSettings.getNamespace(),
                CamelKSupport.camelkCRDContext(kind, CamelKSettings.getApiVersion()), resourceName);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractCamelKAction.Builder<DeleteCamelKResourceAction, Builder> {

        private String kind;
        private String resourceName;

        /**
         * Fluent entry method.
         * @param kind
         * @return
         */
        public static Builder deleteResource(String kind) {
            return new Builder().kind(kind);
        }

        public Builder kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder resource(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        @Override
        public DeleteCamelKResourceAction build() {
            return new DeleteCamelKResourceAction(this);
        }
    }
}
