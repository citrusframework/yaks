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
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBinding;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletBindingList;
import org.citrusframework.yaks.camelk.model.Pipe;
import org.citrusframework.yaks.camelk.model.PipeList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action verifies Camel K binding is present in given namespace.
 *
 * @author Christoph Deppisch
 */
public class VerifyPipeAction extends AbstractKameletAction {

    private final String pipeName;

    private final int maxAttempts;
    private final long delayBetweenAttempts;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyPipeAction(Builder builder) {
        super("verify-pipe", builder);
        this.pipeName = builder.pipeName;
        this.maxAttempts = builder.maxAttempts;
        this.delayBetweenAttempts = builder.delayBetweenAttempts;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(this.pipeName);

        LOG.info(String.format("Verify pipe '%s'", name));

        if (YaksSettings.isLocal(clusterType(context))) {
            verifyLocalPipe(name, context);
        } else {
            verifyPipe(namespace(context), name, context);
        }

        LOG.info(String.format("Successfully verified pipe '%s' - All values OK!", name));
    }

    private void verifyLocalPipe(String name, TestContext context) {
        Long pid = context.getVariable(name + ":pid", Long.class);

        for (int i = 0; i < maxAttempts; i++) {
            Map<String, String> properties = camel().get(pid);
            if ((!properties.isEmpty() && properties.get("STATUS").equals("Running"))) {
                LOG.info(String.format("Verified pipe '%s' state 'Running' - All values OK!", name));
                return;
            }

            LOG.info(String.format("Waiting for pipe '%s' to be in state 'Running'- retry in %s ms", name, delayBetweenAttempts));
            try {
                Thread.sleep(delayBetweenAttempts);
            } catch (InterruptedException e) {
                LOG.warn("Interrupted while waiting for pipe", e);
            }
        }

        throw new ValidationException(String.format("Failed to retrieve pipe '%s' in state 'Running'", name));
    }

    private void verifyPipe(String namespace, String name, TestContext context) {
        Pipe pipe = null;
        for (int i = 0; i < maxAttempts; i++) {
            if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
                pipe = getKubernetesClient().resources(KameletBinding.class, KameletBindingList.class)
                        .inNamespace(namespace)
                        .withName(name)
                        .get();
            } else {
                pipe = getKubernetesClient().resources(Pipe.class, PipeList.class)
                        .inNamespace(namespace)
                        .withName(name)
                        .get();
            }

            if (pipe == null) {
                LOG.info(String.format("Waiting for pipe '%s' - retry in %s ms", name, delayBetweenAttempts));
                try {
                    Thread.sleep(delayBetweenAttempts);
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for pipe", e);
                }
            } else {
                break;
            }
        }

        if (pipe == null) {
            throw new ValidationException(String.format("Failed to retrieve pipe '%s' in namespace '%s'", name, namespace));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml().dumpAsMap(pipe));
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<VerifyPipeAction, Builder> {

        private String pipeName;

        private int maxAttempts = CamelKSettings.getMaxAttempts();
        private long delayBetweenAttempts = CamelKSettings.getDelayBetweenAttempts();


        public Builder isAvailable() {
            return this;
        }

        public Builder isAvailable(String name) {
            this.pipeName = name;
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
        public VerifyPipeAction build() {
            return new VerifyPipeAction(this);
        }
    }
}
