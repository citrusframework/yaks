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
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
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

    private final int maxAttempts;
    private final long delayBetweenAttempts;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyKameletBindingAction(Builder builder) {
        super("verify-kamelet-binding", builder);
        this.bindingName = builder.bindingName;
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
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

        for (int i = 0; i < maxAttempts; i++) {
            Map<String, String> properties = camel().get(pid);
            if ((!properties.isEmpty() && properties.get("STATUS").equals("Running"))) {
                LOG.info(String.format("Verified Kamelet binding '%s' state 'Running' - All values OK!", name));
                return;
            }

            LOG.info(String.format("Waiting for Kamelet binding '%s' to be in state 'Running'- retry in %s ms", name, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for Kamelet binding", e);
            }
        }

        throw new ValidationException(String.format("Failed to retrieve Kamelet binding '%s' in state 'Running'", name));
    }

    private void verifyKameletBinding(String namespace, String name) {
        KameletBinding binding = null;
        for (int i = 0; i < maxAttempts; i++) {
            binding = getKubernetesClient().resources(KameletBinding.class, KameletBindingList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .get();

            if (binding == null) {
                LOG.info(String.format("Waiting for Kamelet binding '%s' - retry in %s ms", name, delayBetweenAttempts));
                try {
                    Thread.sleep(delayBetweenAttempts);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for Kamelet binding", e);
                }
            } else {
                break;
            }
        }

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
