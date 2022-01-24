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

package org.citrusframework.yaks.camelk.actions.integration;

import com.consol.citrus.context.TestContext;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.IntegrationList;

/**
 * @author Christoph Deppisch
 */
public class DeleteIntegrationAction extends AbstractCamelKAction {

    private final String integrationName;

    public DeleteIntegrationAction(Builder builder) {
        super("delete-integration", builder);

        this.integrationName = builder.integrationName;
    }

    @Override
    public void doExecute(TestContext context) {
        CustomResourceDefinitionContext ctx = CamelKSupport.integrationCRDContext(CamelKSettings.getApiVersion());
        getKubernetesClient().customResources(ctx, Integration.class, IntegrationList.class)
                .inNamespace(namespace(context))
                .withName(integrationName)
                .delete();
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractCamelKAction.Builder<DeleteIntegrationAction, Builder> {

        private String integrationName;

        public Builder integration(String integrationName) {
            this.integrationName = integrationName;
            return this;
        }

        @Override
        public DeleteIntegrationAction build() {
            return new DeleteIntegrationAction(this);
        }
    }
}
