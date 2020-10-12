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

package org.citrusframework.yaks.knative.actions.messaging;

import com.consol.citrus.context.TestContext;
import io.fabric8.knative.messaging.v1alpha1.Channel;
import io.fabric8.knative.messaging.v1alpha1.ChannelBuilder;
import org.citrusframework.yaks.knative.KnativeSettings;
import org.citrusframework.yaks.knative.KnativeSupport;
import org.citrusframework.yaks.knative.actions.AbstractKnativeAction;

/**
 * @author Christoph Deppisch
 */
public class CreateChannelAction extends AbstractKnativeAction {

    private final String channelName;

    public CreateChannelAction(Builder builder) {
        super("create-channel", builder);

        this.channelName = builder.channelName;
    }

    @Override
    public void doExecute(TestContext context) {
        Channel channel = new ChannelBuilder()
            .withApiVersion(String.format("messaging.knative.dev/%s", KnativeSupport.knativeApiVersion()))
                .withNewMetadata()
                    .withNamespace(namespace(context))
                    .withName(context.replaceDynamicContentInString(channelName))
                    .withLabels(KnativeSettings.getDefaultLabels())
                .endMetadata()
            .build();

        KnativeSupport.createResource(getKubernetesClient(), namespace(context),
                KnativeSupport.messagingCRDContext("channels", KnativeSupport.knativeApiVersion()), channel);
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKnativeAction.Builder<CreateChannelAction, Builder> {

        private String channelName;

        public Builder name(String channelName) {
            this.channelName = channelName;
            return this;
        }

        @Override
        public CreateChannelAction build() {
            return new CreateChannelAction(this);
        }
    }
}
