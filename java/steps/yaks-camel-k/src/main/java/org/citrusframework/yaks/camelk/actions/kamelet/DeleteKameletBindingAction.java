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

import com.consol.citrus.context.TestContext;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.KameletBinding;
import org.citrusframework.yaks.camelk.model.KameletBindingList;

/**
 * @author Christoph Deppisch
 */
public class DeleteKameletBindingAction extends AbstractCamelKAction {

    private final String name;

    public DeleteKameletBindingAction(Builder builder) {
        super("delete-kamelet-binding", builder);

        this.name = builder.name;
    }

    @Override
    public void doExecute(TestContext context) {
        String bindingName = context.replaceDynamicContentInString(name);
        CustomResourceDefinitionContext ctx = CamelKSupport.kameletBindingCRDContext(CamelKSettings.getKameletApiVersion());
        getKubernetesClient().customResources(ctx, KameletBinding.class, KameletBindingList.class)
                .inNamespace(CamelKSettings.getNamespace())
                .withName(bindingName)
                .delete();
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractCamelKAction.Builder<DeleteKameletBindingAction, Builder> {

        private String name;

        public Builder binding(String name) {
            this.name = name;
            return this;
        }

        @Override
        public DeleteKameletBindingAction build() {
            return new DeleteKameletBindingAction(this);
        }
    }
}
