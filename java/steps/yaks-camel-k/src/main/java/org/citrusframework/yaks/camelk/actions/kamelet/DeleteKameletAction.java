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
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletList;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletV1Alpha1;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletV1Alpha1List;

/**
 * @author Christoph Deppisch
 */
public class DeleteKameletAction extends AbstractKameletAction {

    private final String kameletName;

    public DeleteKameletAction(Builder builder) {
        super("delete-kamelet", builder);

        this.kameletName = builder.kameletName;
    }

    @Override
    public void doExecute(TestContext context) {
        String kameletName = context.replaceDynamicContentInString(this.kameletName);
        if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
            getKubernetesClient().resources(KameletV1Alpha1.class, KameletV1Alpha1List.class)
                    .inNamespace(kameletNamespace(context))
                    .withName(kameletName)
                    .delete();
        } else {
            getKubernetesClient().resources(Kamelet.class, KameletList.class)
                    .inNamespace(kameletNamespace(context))
                    .withName(kameletName)
                    .delete();
        }
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKameletAction.Builder<DeleteKameletAction, Builder> {

        private String kameletName;

        public Builder kamelet(String name) {
            this.kameletName = name;
            return this;
        }

        @Override
        public DeleteKameletAction build() {
            return new DeleteKameletAction(this);
        }
    }
}
