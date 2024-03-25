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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.citrusframework.context.TestContext;

/**
 * @author Christoph Deppisch
 */
public class CreateResourceAction extends AbstractKubernetesAction implements KubernetesAction {

    private final String content;

    public CreateResourceAction(Builder builder) {
        super("create-resource", builder);
        this.content = builder.content;
    }

    @Override
    public void doExecute(TestContext context) {
        getKubernetesClient()
                 .load(new ByteArrayInputStream(context.replaceDynamicContentInString(content)
                         .getBytes(StandardCharsets.UTF_8)))
                 .inNamespace(namespace(context))
                 .createOrReplace();
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKubernetesAction.Builder<CreateResourceAction, Builder> {

        private String content;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        @Override
        public CreateResourceAction build() {
            return new CreateResourceAction(this);
        }
    }
}
