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
import org.citrusframework.yaks.camelk.VariableNames;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;

/**
 * @author Christoph Deppisch
 */
public abstract class AbstractKameletAction extends AbstractCamelKAction {

    public AbstractKameletAction(String name, Builder<?, ?> builder) {
        super(name, builder);
    }

    @Override
    public String namespace(TestContext context) {
        if (context.getVariables().containsKey(VariableNames.KAMELET_NAMESPACE.value())) {
            return context.getVariable(VariableNames.KAMELET_NAMESPACE.value());
        }

        return super.namespace(context);
    }
}
