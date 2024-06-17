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

import java.util.Map;

import org.apache.camel.v1alpha1.KameletBinding;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.ValidationException;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBindingList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action verifies Camel K binding is present in given namespace.
 *
 * @author Christoph Deppisch
 */
public class VerifyKameletBindingAction extends AbstractKameletAction {

    private final String bindingName;

    private final int maxAttempts;
    private final long delayBetweenAttempts;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyKameletBindingAction(Builder builder) {
        super("verify-binding", builder);
        this.bindingName = builder.bindingName;
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(this.bindingName);

        LOG.info(String.format("Verify binding '%s'", name));

        if (YaksSettings.isLocal(clusterType(context))) {
            verifyLocalKameletBinding(name, context);
        } else {
            verifyKameletBinding(namespace(context), name, context);
        }

        LOG.info(String.format("Successfully verified binding '%s' - All values OK!", name));
    }

    private void verifyLocalKameletBinding(String name, TestContext context) {
        Long pid = context.getVariable(name + ":pid", Long.class);

        for (int i = 0; i < maxAttempts; i++) {
            Map<String, String> properties = camel().get(pid);
            if ((!properties.isEmpty() && properties.get("STATUS").equals("Running"))) {
                LOG.info(String.format("Verified binding '%s' state 'Running' - All values OK!", name));
                return;
            }

            LOG.info(String.format("Waiting for binding '%s' to be in state 'Running'- retry in %s ms", name, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for binding", e);
            }
        }

        throw new ValidationException(String.format("Failed to retrieve binding '%s' in state 'Running'", name));
    }

    private void verifyKameletBinding(String namespace, String name, TestContext context) {
        KameletBinding binding = null;
        for (int i = 0; i < maxAttempts; i++) {
            binding = getKubernetesClient().resources(KameletBinding.class, KameletBindingList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .get();

            if (binding == null) {
                LOG.info(String.format("Waiting for binding '%s' - retry in %s ms", name, delayBetweenAttempts));
                try {
                    Thread.sleep(delayBetweenAttempts);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for binding", e);
                }
            } else {
                break;
            }
        }

        if (binding == null) {
            throw new ValidationException(String.format("Failed to retrieve binding '%s' in namespace '%s'", name, namespace));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml(new KameletBindingValuePropertyMapper()).dumpAsMap(binding));
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<VerifyKameletBindingAction, Builder> {

        private String bindingName;

        private int maxAttempts = CamelKSettings.getMaxAttempts();
        private long delayBetweenAttempts = CamelKSettings.getDelayBetweenAttempts();

        public Builder isAvailable() {
            return this;
        }

        public Builder isAvailable(String name) {
            this.bindingName = name;
            return this;
        }

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder delayBetweenAttempts(long delayBetweenAttempts) {
            this.delayBetweenAttempts = delayBetweenAttempts;
            return this;
        }

        @Override
        public VerifyKameletBindingAction build() {
            return new VerifyKameletBindingAction(this);
        }
    }
}
