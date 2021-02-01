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
import com.consol.citrus.exceptions.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.DoneableKamelet;
import org.citrusframework.yaks.camelk.model.Kamelet;
import org.citrusframework.yaks.camelk.model.KameletList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * Test action verifies Kamelet CRD is present on given namespace.
 *
 * @author Christoph Deppisch
 */
public class VerifyKameletAction extends AbstractCamelKAction {

    private final String name;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public VerifyKameletAction(Builder builder) {
        super("verify-kamelet", builder);
        this.name = builder.name;
    }

    @Override
    public void doExecute(TestContext context) {
        String kameletName = context.replaceDynamicContentInString(name);
        CustomResourceDefinitionContext ctx = CamelKSupport.kameletCRDContext(CamelKSettings.getKameletApiVersion());
        Kamelet kamelet = getKubernetesClient().customResources(ctx, Kamelet.class, KameletList.class, DoneableKamelet.class)
                .inNamespace(CamelKSettings.getNamespace())
                .withName(kameletName)
                .get();

        if (kamelet == null) {
            throw new ValidationException(String.format("Failed to retrieve Kamelet '%s' in namespace '%s'", name, CamelKSettings.getNamespace()));
        }

        LOG.info("Kamlet validation successful - All values OK!");
        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug(KubernetesSupport.json().writeValueAsString(kamelet));
            } catch (JsonProcessingException e) {
                LOG.warn("Unable to dump Kamelet data", e);
            }
        }
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<VerifyKameletAction, Builder> {

        private String name;

        public Builder isAvailable() {
            return this;
        }

        public Builder kameletName(String kameletName) {
            this.name = kameletName;
            return this;
        }

        @Override
        public VerifyKameletAction build() {
            return new VerifyKameletAction(this);
        }
    }
}
