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

package org.citrusframework.yaks.knative;

import org.citrusframework.Citrus;
import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

/**
 * @author Christoph Deppisch
 */
public final class KnativeSupport {

    private KnativeSupport() {
        // prevent instantiation of utility class
    }

    public static KnativeClient getKnativeClient(Citrus citrus) {
        if (citrus.getCitrusContext().getReferenceResolver().resolveAll(KnativeClient.class).size() == 1L) {
            return citrus.getCitrusContext().getReferenceResolver().resolve(KnativeClient.class);
        } else {
            return new DefaultKnativeClient();
        }
    }

    public static CustomResourceDefinitionContext knativeCRDContext(String knativeComponent, String kind, String version) {
        return new CustomResourceDefinitionContext.Builder()
                .withName(String.format("%s.%s.knative.dev", kind, knativeComponent))
                .withGroup(String.format("%s.knative.dev", knativeComponent))
                .withVersion(version)
                .withPlural(kind)
                .withScope("Namespaced")
                .build();
    }

    public static String knativeApiVersion() {
        return KnativeSettings.getApiVersion();
    }

    public static String knativeMessagingGroup() {
        return KnativeSettings.getKnativeMessagingGroup();
    }

    public static String knativeEventingGroup() {
        return KnativeSettings.getKnativeEventingGroup();
    }
}
