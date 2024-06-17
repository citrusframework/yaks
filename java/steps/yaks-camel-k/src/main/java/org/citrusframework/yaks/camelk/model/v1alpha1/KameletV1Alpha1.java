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

package org.citrusframework.yaks.camelk.model.v1alpha1;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.camel.v1alpha1.Kamelet;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;

/**
 * @author Christoph Deppisch
 */
public class KameletV1Alpha1 extends Kamelet {

    public static KameletV1Alpha1 from(org.apache.camel.v1.Kamelet kamelet) {
        try {
            String kameletJson = KubernetesSupport.json().writerFor(org.apache.camel.v1.Kamelet.class).writeValueAsString(kamelet);

            kameletJson = kameletJson.replaceAll("v1", "v1alpha1");

            return KubernetesSupport.json().readValue(kameletJson, KameletV1Alpha1.class);
        } catch (JsonProcessingException e) {
            throw new CitrusRuntimeException("Failed to convert Kamelet to version 'v1alpha1'", e);
        }
    }
}
