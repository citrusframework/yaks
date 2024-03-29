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

package org.citrusframework.yaks.camelk.actions;

import org.citrusframework.TestAction;
import org.citrusframework.context.TestContext;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.citrusframework.yaks.YaksClusterType;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.VariableNames;
import org.citrusframework.yaks.kubernetes.KubernetesSettings;

/**
 * Base action provides access to Knative properties such as broker name. These properties are read from
 * environment settings or explicitly set as part of the test case and get stored as test variables in the current context.
 * This base class gives convenient access to the test variables and provides a fallback if no variable is set.
 *
 * @author Christoph Deppisch
 */
public interface CamelKAction extends TestAction {

    /**
     * Gets the Kubernetes client.
     * @return
     */
    KubernetesClient getKubernetesClient();

    /**
     * Resolves namespace name from given test context using the stored test variable.
     * Fallback to the namespace given in Kubernetes environment settings when no test variable is present.
     *
     * @param context
     * @return
     */
    default String namespace(TestContext context) {
        if (context.getVariables().containsKey(VariableNames.CAMELK_NAMESPACE.value())) {
            return context.getVariable(VariableNames.CAMELK_NAMESPACE.value());
        }

        return KubernetesSettings.getNamespace();
    }

    /**
     * Resolves Camel K operator namespace name from given test context using the stored test variable.
     * Fallback to the namespace given in Camel K environment settings when no test variable is present.
     *
     * @param context
     * @return
     */
    default String operatorNamespace(TestContext context) {
        if (context.getVariables().containsKey(VariableNames.OPERATOR_NAMESPACE.value())) {
            return context.getVariable(VariableNames.OPERATOR_NAMESPACE.value());
        }

        return CamelKSettings.getOperatorNamespace();
    }

    /**
     * Resolves cluster type from given test context using the stored test variable.
     * Fallback to retrieving the cluster type from environment settings when no test variable is present.
     *
     * @param context
     * @return
     */
    default YaksClusterType clusterType(TestContext context) {
        if (context.getVariables().containsKey(VariableNames.CLUSTER_TYPE.value())) {
            Object clusterType = context.getVariableObject(VariableNames.CLUSTER_TYPE.value());

            if (clusterType instanceof YaksClusterType) {
                return (YaksClusterType) clusterType;
            } else {
                return YaksClusterType.valueOf(clusterType.toString());
            }
        }

        return YaksSettings.getClusterType();
    }
}

