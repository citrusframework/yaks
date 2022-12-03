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

import java.util.Map;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ValidationException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.model.KameletBinding;
import org.citrusframework.yaks.camelk.model.KameletBindingList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action verifies Kamelet binding is present in given namespace.
 *
 * @author Christoph Deppisch
 */
public class VerifyKameletBindingAction extends AbstractKameletAction {

    private final String bindingName;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyKameletBindingAction(Builder builder) {
        super("verify-kamelet-binding", builder);
        this.bindingName = builder.bindingName;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(this.bindingName);

        LOG.info(String.format("Verify Kamelet binding '%s'", name));

        if (YaksSettings.isLocal(clusterType(context))) {
            verifyLocalKameletBinding(name, context);
        } else {
            verifyKameletBinding(namespace(context), name);
        }

        LOG.info(String.format("Successfully verified Kamelet binding '%s' - All values OK!", name));
    }

    private void verifyLocalKameletBinding(String name, TestContext context) {
        Long pid = context.getVariable(name + ":pid", Long.class);
        Map<String, String> properties = camel().get(pid);
        if ((!properties.isEmpty() && properties.get("STATUS").equals("Running"))) {
            LOG.info(String.format("Verified Kamelet binding '%s' state 'Running' - All values OK!", name));
        } else {
            throw new ValidationException(String.format("Failed to retrieve Kamelet binding '%s' in state 'Running'", name));
        }
    }

    private void verifyKameletBinding(String namespace, String name) {
        CustomResourceDefinitionContext ctx = CamelKSupport.kameletBindingCRDContext(CamelKSettings.getKameletApiVersion());
        KameletBinding binding = getKubernetesClient().customResources(ctx, KameletBinding.class, KameletBindingList.class)
                .inNamespace(namespace)
                .withName(name)
                .get();

        if (binding == null) {
            throw new ValidationException(String.format("Failed to retrieve Kamelet binding '%s' in namespace '%s'", name, namespace));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml().dumpAsMap(binding));
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<VerifyKameletBindingAction, Builder> {

        private String bindingName;

        public Builder isAvailable() {
            return this;
        }

        public Builder isAvailable(String name) {
            this.bindingName = name;
            return this;
        }

        @Override
        public VerifyKameletBindingAction build() {
            return new VerifyKameletBindingAction(this);
        }
    }
}
