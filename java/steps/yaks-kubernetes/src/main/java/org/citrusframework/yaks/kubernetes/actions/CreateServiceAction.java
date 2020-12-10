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

package org.citrusframework.yaks.kubernetes.actions;

import java.util.Collections;

import com.consol.citrus.context.TestContext;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.kubernetes.KubernetesSettings;

/**
 * @author Christoph Deppisch
 */
public class CreateServiceAction extends AbstractKubernetesAction {

    private final String serviceName;
    private final String port;
    private final String targetPort;
    private final String protocol;

    public CreateServiceAction(Builder builder) {
        super("create-service", builder);

        this.serviceName = builder.serviceName;
        this.port = builder.port;
        this.targetPort = builder.targetPort;
        this.protocol = builder.protocol;
    }

    @Override
    public void doExecute(TestContext context) {
        getKubernetesClient().services().inNamespace(namespace(context)).createOrReplaceWithNew()
                .withNewMetadata()
                    .withNamespace(namespace(context))
                    .withName(context.replaceDynamicContentInString(serviceName))
                    .withLabels(KubernetesSettings.getDefaultLabels())
                .endMetadata()
                .withNewSpec()
                    // add selector to the very specific Pod that is running the test right now. This way the service will route all traffic to the test
                    .withSelector(Collections.singletonMap("yaks.citrusframework.org/test-id", YaksSettings.getTestId()))
                    .withPorts(new ServicePortBuilder()
                            .withProtocol(context.replaceDynamicContentInString(protocol))
                            .withPort(Integer.parseInt(context.replaceDynamicContentInString(port)))
                            .withTargetPort(new IntOrString(Integer.parseInt(context.replaceDynamicContentInString(targetPort))))
                            .build())
                .endSpec()
                .done();
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKubernetesAction.Builder<CreateServiceAction, Builder> {

        private String serviceName;
        private String port = "80";
        private String targetPort = "8080";
        private String protocol = "TCP";

        public Builder name(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder port(int port) {
            this.port = String.valueOf(port);
            return this;
        }

        public Builder targetPort(String targetPort) {
            this.targetPort = targetPort;
            return this;
        }

        public Builder targetPort(int targetPort) {
            this.targetPort = String.valueOf(targetPort);
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public CreateServiceAction build() {
            return new CreateServiceAction(this);
        }
    }
}
