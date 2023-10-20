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

package org.citrusframework.yaks.knative.actions;

import org.citrusframework.AbstractTestActionBuilder;
import org.citrusframework.actions.AbstractTestAction;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractKnativeAction extends AbstractTestAction implements KnativeAction {

    /** Logger */
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private final KnativeClient knativeClient;
    private final KubernetesClient kubernetesClient;

    public AbstractKnativeAction(String name, Builder<?, ?> builder) {
        super("knative:" + name, builder);

        this.knativeClient = builder.knativeClient;
        this.kubernetesClient = builder.kubernetesClient;
    }

    @Override
    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    @Override
    public KnativeClient getKnativeClient() {
        return knativeClient;
    }

    /**
     * Action builder.
     */
    public static abstract class Builder<T extends KnativeAction, B extends Builder<T, B>> extends AbstractTestActionBuilder<T, B> {

        private KnativeClient knativeClient;
        private KubernetesClient kubernetesClient;

        /**
         * Use a custom Kubernetes client.
         */
        public B client(KubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
            return self;
        }

        /**
         * Use a custom Knative client.
         */
        public B client(KnativeClient knativeClient) {
            this.knativeClient = knativeClient;
            return self;
        }

    }
}
