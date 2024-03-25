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

package org.citrusframework.yaks.kubernetes.actions;

import org.citrusframework.AbstractTestActionBuilder;
import org.citrusframework.actions.AbstractTestAction;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.kubernetes.KubernetesActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractKubernetesAction extends AbstractTestAction implements KubernetesAction {

    /** Logger */
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    public AbstractKubernetesAction(String name, Builder<?, ?> builder) {
        super("k8s:" + name, builder);

        this.kubernetesClient = builder.kubernetesClient;
        this.setActor(builder.getActor());
    }

    @Override
    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    /**
     * Action builder.
     */
    public static abstract class Builder<T extends KubernetesAction, B extends Builder<T, B>> extends AbstractTestActionBuilder<T, B> {

        private KubernetesClient kubernetesClient;

        public Builder() {
            actor(new KubernetesActor());
        }

        /**
         * Use a custom Kubernetes client.
         */
        public B client(KubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
            return self;
        }

    }
}
