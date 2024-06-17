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

package org.citrusframework.yaks.camelk.actions.kamelet;

import java.util.Collections;

import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.yaml.snakeyaml.introspector.Property;

/**
 * Helper to properly handle additional properties on binding additional properties.
 */
class KameletBindingValuePropertyMapper implements KubernetesSupport.PropertyValueMapper {
    @Override
    public Object map(Property property, Object propertyValue) {
        if (propertyValue == null) {
            return null;
        }

        if (property.getName().equals("additionalProperties")) {
            return Collections.emptyMap();
        }

        if (propertyValue instanceof org.apache.camel.v1alpha1.kameletbindingspec.source.Properties sourceProps) {
            return sourceProps.getAdditionalProperties();
        }

        if (propertyValue instanceof org.apache.camel.v1alpha1.kameletbindingspec.steps.Properties sourceProps) {
            return sourceProps.getAdditionalProperties();
        }

        if (propertyValue instanceof org.apache.camel.v1alpha1.kameletbindingspec.sink.Properties sinkProps) {
            return sinkProps.getAdditionalProperties();
        }

        if (propertyValue instanceof org.apache.camel.v1alpha1.kameletbindingspec.ErrorHandler errorHandlerProps) {
            return errorHandlerProps.getAdditionalProperties();
        }

        return propertyValue;
    }
}
