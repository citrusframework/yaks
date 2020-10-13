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

package org.citrusframework.yaks.knative.actions.eventing;

import com.consol.citrus.context.TestContext;
import io.fabric8.knative.eventing.v1alpha1.Broker;
import io.fabric8.knative.eventing.v1alpha1.BrokerBuilder;
import org.citrusframework.yaks.knative.KnativeSettings;
import org.citrusframework.yaks.knative.KnativeSupport;
import org.citrusframework.yaks.knative.actions.AbstractKnativeAction;

/**
 * @author Christoph Deppisch
 */
public class CreateBrokerAction extends AbstractKnativeAction {

    private final String brokerName;

    public CreateBrokerAction(Builder builder) {
        super("create-broker", builder);

        this.brokerName = builder.brokerName;
    }

    @Override
    public void doExecute(TestContext context) {
        Broker broker = new BrokerBuilder()
                .withApiVersion(String.format("eventing.knative.dev/%s", KnativeSupport.knativeApiVersion()))
                .withNewMetadata()
                .withNamespace(namespace(context))
                .withName(context.replaceDynamicContentInString(brokerName))
                .withLabels(KnativeSettings.getDefaultLabels())
                .endMetadata()
                .build();

        KnativeSupport.createResource(getKubernetesClient(), namespace(context),
                KnativeSupport.eventingCRDContext("brokers", KnativeSupport.knativeApiVersion()), broker);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKnativeAction.Builder<CreateBrokerAction, Builder> {

        private String brokerName;

        public Builder name(String brokerName) {
            this.brokerName = brokerName;
            return this;
        }

        @Override
        public CreateBrokerAction build() {
            return new CreateBrokerAction(this);
        }
    }
}
