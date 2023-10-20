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

import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.ValidationException;
import io.fabric8.knative.eventing.v1.Broker;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.citrusframework.yaks.knative.actions.AbstractKnativeAction;

/**
 * @author Christoph Deppisch
 */
public class VerifyBrokerAction extends AbstractKnativeAction {

    private final String brokerName;

    public VerifyBrokerAction(Builder builder) {
        super("verify-broker", builder);

        this.brokerName = builder.brokerName;
    }

    @Override
    public void doExecute(TestContext context) {
        try {
            Broker broker = getKnativeClient().brokers()
                    .inNamespace(namespace(context))
                    .withName(brokerName)
                    .get();

            if (broker.getStatus() != null &&
                    broker.getStatus().getConditions() != null &&
                    broker.getStatus().getConditions().stream()
                            .anyMatch(condition -> condition.getType().equals("Ready") &&
                                    condition.getStatus().equalsIgnoreCase("True"))) {
                LOG.info(String.format("Knative broker %s is ready", brokerName));
            } else {
                throw new ValidationException(String.format("Knative broker '%s' is not ready", brokerName));
            }
        } catch (KubernetesClientException e) {
            throw new ValidationException(String.format("Failed to validate Knative broker '%s' - " +
                    "not found in namespace '%s'", brokerName, namespace(context)), e);
        }
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKnativeAction.Builder<VerifyBrokerAction, Builder> {

        private String brokerName;

        public Builder name(String brokerName) {
            this.brokerName = brokerName;
            return this;
        }

        @Override
        public VerifyBrokerAction build() {
            return new VerifyBrokerAction(this);
        }
    }
}
