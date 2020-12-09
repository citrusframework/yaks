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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.consol.citrus.context.TestContext;

/**
 * @author Christoph Deppisch
 */
public class DeleteResourceAction extends AbstractKubernetesAction {

    private final String content;

    public DeleteResourceAction(Builder builder) {
        super("create-resource", builder);
        this.content = builder.content;
    }

    @Override
    public void doExecute(TestContext context) {
        getKubernetesClient()
                .load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .inNamespace(namespace(context))
                .delete();
    }

    /**
     * Action builder.
     */
    public static class Builder extends AbstractKubernetesAction.Builder<DeleteResourceAction, Builder> {

        private String content;

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        @Override
        public DeleteResourceAction build() {
            return new DeleteResourceAction(this);
        }
    }
}
