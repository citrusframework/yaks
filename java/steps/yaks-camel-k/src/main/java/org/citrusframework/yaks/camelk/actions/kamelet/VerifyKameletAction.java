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

import java.util.Arrays;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.apache.camel.v1.Kamelet;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.ValidationException;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.model.KameletList;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletV1Alpha1;
import org.citrusframework.yaks.camelk.model.v1alpha1.KameletV1Alpha1List;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * Test action verifies Kamelet CRD is present on given namespace.
 *
 * @author Christoph Deppisch
 */
public class VerifyKameletAction extends AbstractKameletAction {

    private final String kameletName;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyKameletAction(Builder builder) {
        super("verify-kamelet", builder);
        this.kameletName = builder.kameletName;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(this.kameletName);

        if (findKamelet(name, context, kameletNamespace(context), operatorNamespace(context), namespace(context)))  {
            LOG.info("Kamlet validation successful - All values OK!");
        } else {
            throw new ValidationException(String.format("Failed to retrieve Kamelet '%s'", name));
        }
    }

    @Override
    public boolean isDisabled(TestContext context) {
        return YaksSettings.isLocal(clusterType(context));
    }

    /**
     * Find Kamelet with specified name in one of the given namespaces.
     * @param name
     * @param context
     * @param namespaces
     * @return
     */
    private boolean findKamelet(String name, TestContext context, String ... namespaces) {
        return Arrays.stream(namespaces).distinct().anyMatch(namespace -> {
            LOG.info(String.format("Verify Kamlet '%s' exists in namespace '%s'", name, namespace));

            HasMetadata kamelet;
            if (getApiVersion(context).equals(CamelKSettings.V1ALPHA1)) {
                kamelet = getKubernetesClient().resources(KameletV1Alpha1.class, KameletV1Alpha1List.class)
                        .inNamespace(namespace)
                        .withName(name)
                        .get();
            } else {
                kamelet = getKubernetesClient().resources(Kamelet.class, KameletList.class)
                    .inNamespace(namespace)
                    .withName(name)
                    .get();
            }

            if (LOG.isDebugEnabled()) {
                if (kamelet == null) {
                    LOG.debug(String.format("Kamelet '%s' is not present in namespace '%s'", name, namespace));
                } else {
                    LOG.debug(String.format("Found Kamelet in namespace '%s'", namespace));
                    LOG.debug(KubernetesSupport.dumpYaml(kamelet));
                }
            }

            return kamelet != null;
        });
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractKameletAction.Builder<VerifyKameletAction, Builder> {

        private String kameletName;

        public Builder isAvailable() {
            return this;
        }

        public Builder kameletName(String kameletName) {
            this.kameletName = kameletName;
            return this;
        }

        @Override
        public VerifyKameletAction build() {
            return new VerifyKameletAction(this);
        }
    }
}
