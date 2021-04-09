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

import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.context.TestContext;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.knative.eventing.v1.TriggerSpecBuilder;
import org.citrusframework.yaks.knative.KnativeSettings;
import org.citrusframework.yaks.knative.KnativeSupport;
import org.citrusframework.yaks.knative.actions.AbstractKnativeAction;

/**
 * @author Christoph Deppisch
 */
public class CreateTriggerAction extends AbstractKnativeAction {

    private final String triggerName;
    private final String serviceName;
    private final String channelName;

    private final Map<String, String> filterOnAttributes;

    public CreateTriggerAction(Builder builder) {
        super("create-trigger", builder);

        this.triggerName = builder.triggerName;
        this.serviceName = builder.serviceName;
        this.channelName = builder.channelName;
        this.filterOnAttributes = builder.filterOnAttributes;
    }

    @Override
    public void doExecute(TestContext context) {
        TriggerSpecBuilder triggerSpec = new TriggerSpecBuilder()
                .withBroker(brokerName(context));

        addServiceSubscriber(triggerSpec, context);
        addChannelSubscriber(triggerSpec, context);
        addFilterOnAttributes(triggerSpec, context);

        Trigger trigger = new TriggerBuilder()
                .withApiVersion(String.format("%s/%s", KnativeSupport.knativeEventingGroup(), KnativeSupport.knativeApiVersion()))
                .withNewMetadata()
                    .withNamespace(namespace(context))
                    .withName(context.replaceDynamicContentInString(triggerName))
                    .withLabels(KnativeSettings.getDefaultLabels())
                .endMetadata()
                .withSpec(triggerSpec.build())
                .build();

        getKnativeClient().triggers()
                .inNamespace(namespace(context))
                .createOrReplace(trigger);
    }

    private void addFilterOnAttributes(TriggerSpecBuilder triggerSpec, TestContext context) {
        if (!filterOnAttributes.isEmpty()) {
            triggerSpec.withNewFilter()
                    .withAttributes(context.resolveDynamicValuesInMap(filterOnAttributes))
                    .endFilter();
        }
    }

    private void addChannelSubscriber(TriggerSpecBuilder triggerSpec, TestContext context) {
        if (channelName != null) {
            triggerSpec.withNewSubscriber()
                    .withNewRef()
                        .withApiVersion(String.format("%s/%s", KnativeSupport.knativeMessagingGroup(), KnativeSupport.knativeApiVersion()))
                        .withKind("InMemoryChannel")
                        .withName(context.replaceDynamicContentInString(channelName))
                    .endRef()
                    .endSubscriber();
        }
    }

    private void addServiceSubscriber(TriggerSpecBuilder triggerSpec, TestContext context) {
        if (serviceName != null) {
            triggerSpec.withNewSubscriber()
                    .withNewRef()
                        .withApiVersion("v1")
                        .withKind("Service")
                        .withName(context.replaceDynamicContentInString(serviceName))
                    .endRef()
                    .endSubscriber();
        }
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKnativeAction.Builder<CreateTriggerAction, Builder> {

        private String triggerName;
        private String serviceName;
        private String channelName;

        private Map<String, String> filterOnAttributes = new HashMap<>();

        public Builder name(String triggerName) {
            this.triggerName = triggerName;
            return this;
        }

        public Builder onService(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder onChannel(String channelName) {
            this.channelName = channelName;
            return this;
        }

        public Builder filter(Map<String, String> filter) {
            this.filterOnAttributes.putAll(filter);
            return this;
        }

        public Builder filter(String attributeName, String value) {
            this.filterOnAttributes.put(attributeName, value);
            return this;
        }

        @Override
        public CreateTriggerAction build() {
            return new CreateTriggerAction(this);
        }
    }
}
