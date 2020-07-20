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

import java.io.IOException;

import com.consol.citrus.Citrus;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author Christoph Deppisch
 */
public final class KnativeSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private KnativeSupport() {
        // prevent instantiation of utility class
    }

    static KubernetesClient getKubernetesClient(Citrus citrus) {
        if (citrus.getCitrusContext().getReferenceResolver().resolveAll(KubernetesClient.class).size() == 1L) {
            return citrus.getCitrusContext().getReferenceResolver().resolve(KubernetesClient.class);
        } else {
            return new DefaultKubernetesClient();
        }
    }

    static KnativeClient getKnativeClient(Citrus citrus) {
        if (citrus.getCitrusContext().getReferenceResolver().resolveAll(KnativeClient.class).size() == 1L) {
            return citrus.getCitrusContext().getReferenceResolver().resolve(KnativeClient.class);
        } else {
            return new DefaultKnativeClient();
        }
    }

    static Yaml yaml() {
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                // if value of property is null, ignore it.
                if (propertyValue == null) {
                    return null;
                }
                else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };
        return new Yaml(representer);
    }

    static ObjectMapper json() {
        return OBJECT_MAPPER;
    }

    static <T> void createResource(KubernetesClient k8sClient, String namespace,
                                   CustomResourceDefinitionContext context, T resource) {
        try {
            k8sClient.customResource(context).createOrReplace(namespace, KnativeSupport.yaml().dump(resource));
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to create Knative resource", e);
        }
    }

    public static void deleteResource(KubernetesClient k8sClient, String namespace,
                                      CustomResourceDefinitionContext context, String resourceName) {
        try {
            k8sClient.customResource(context).delete(namespace, resourceName);
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to delete Knative resource", e);
        }
    }

    static CustomResourceDefinitionContext eventingCRDContext(String resourceName) {
        return new CustomResourceDefinitionContext.Builder()
                .withName(String.format("%s.eventing.knative.dev", resourceName))
                .withGroup("eventing.knative.dev")
                .withVersion("v1")
                .withPlural(resourceName)
                .withScope("Namespaced")
                .build();
    }

    static CustomResourceDefinitionContext messagingCRDContext(String resourceName) {
        return new CustomResourceDefinitionContext.Builder()
                .withName(String.format("%s.messaging.knative.dev", resourceName))
                .withGroup("messaging.knative.dev")
                .withVersion("v1")
                .withPlural(resourceName)
                .withScope("Namespaced")
                .build();
    }
}
