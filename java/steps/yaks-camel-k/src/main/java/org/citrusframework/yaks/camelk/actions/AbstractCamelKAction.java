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

package org.citrusframework.yaks.camelk.actions;

import com.consol.citrus.AbstractTestActionBuilder;
import com.consol.citrus.actions.AbstractTestAction;
import com.consol.citrus.context.TestContext;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.YaksClusterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractCamelKAction extends AbstractTestAction implements CamelKAction {

    /** Logger */
    protected final Logger LOG = LoggerFactory.getLogger(getClass());

    private final String namespace;

    private final KubernetesClient kubernetesClient;

    private final YaksClusterType clusterType;

    public AbstractCamelKAction(String name, Builder<?, ?> builder) {
        super("camel-k:" + name, builder);

        this.namespace = builder.namespace;
        this.kubernetesClient = builder.kubernetesClient;
        this.clusterType = builder.clusterType;
    }

    @Override
    public KubernetesClient getKubernetesClient() {
        return kubernetesClient;
    }

    @Override
    public String namespace(TestContext context) {
        if (namespace != null) {
            return context.replaceDynamicContentInString(namespace);
        }

        return CamelKAction.super.namespace(context);
    }

    @Override
    public YaksClusterType clusterType(TestContext context) {
        if (clusterType != null) {
            return clusterType;
        }

        return CamelKAction.super.clusterType(context);
    }

    /**
     * Action builder.
     */
    public static abstract class Builder<T extends CamelKAction, B extends Builder<T, B>> extends AbstractTestActionBuilder<T, B> {

        private KubernetesClient kubernetesClient;

        private String namespace;

        private YaksClusterType clusterType;

        /**
         * Use a custom Kubernetes client.
         */
        public B client(KubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
            return self;
        }

        /**
         * Explicitly set namespace for this action.
         */
        public B namespace(String namespace) {
            this.namespace = namespace;
            return self;
        }

        /**
         * Explicitly set cluster type for this action.
         */
        public B clusterType(YaksClusterType clusterType) {
            this.clusterType = clusterType;
            return self;
        }

    }
}
