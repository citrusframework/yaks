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

package org.citrusframework.yaks.kubernetes;

import java.io.IOException;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
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
public final class KubernetesSupport {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES);
    }

    private KubernetesSupport() {
        // prevent instantiation of utility class
    }

    public static KubernetesClient getKubernetesClient(Citrus citrus) {
        if (citrus.getCitrusContext().getReferenceResolver().resolveAll(KubernetesClient.class).size() == 1L) {
            return citrus.getCitrusContext().getReferenceResolver().resolve(KubernetesClient.class);
        } else {
            return new DefaultKubernetesClient();
        }
    }

    public static Yaml yaml() {
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
        representer.getPropertyUtils().setSkipMissingProperties(true);
        return new Yaml(representer);
    }

    public static ObjectMapper json() {
        return OBJECT_MAPPER;
    }

    public static Map<String, Object> getResource(KubernetesClient k8sClient, String namespace,
                                                  CustomResourceDefinitionContext context, String resourceName) {
        return k8sClient.customResource(context).get(namespace, resourceName);
    }

    public static Map<String, Object> getResources(KubernetesClient k8sClient, String namespace,
                                                  CustomResourceDefinitionContext context) {
        return k8sClient.customResource(context).list(namespace);
    }

    public static <T> void createResource(KubernetesClient k8sClient, String namespace,
                                   CustomResourceDefinitionContext context, T resource) {
        createResource(k8sClient, namespace, context, yaml().dump(resource));
    }

    public static void createResource(KubernetesClient k8sClient, String namespace,
                                   CustomResourceDefinitionContext context, String yaml) {
        try {
            k8sClient.customResource(context).createOrReplace(namespace, yaml);
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

    public static CustomResourceDefinitionContext crdContext(String resourceType, String group, String kind, String version) {
        return new CustomResourceDefinitionContext.Builder()
                .withName(resourceType.contains(".") ? resourceType : String.format("%s.%s", resourceType, group))
                .withGroup(group)
                .withKind(kind)
                .withVersion(version)
                .withPlural(resourceType.contains(".") ? resourceType.substring(0, resourceType.indexOf(".")) : resourceType)
                .withScope("Namespaced")
                .build();
    }

    public static String kubernetesApiVersion() {
        return KubernetesSettings.getApiVersion();
    }

    /**
     * Checks pod status with expected phase. If expected status is "Running" all
     * containers in the pod must be in ready state, too.
     * @param pod
     * @param status
     * @return
     */
    public static boolean verifyPodStatus(Pod pod, String status) {
        if (pod == null || pod.getStatus() == null ||
                !status.equals(pod.getStatus().getPhase())) {
            return false;
        }

        return !status.equals("Running") ||
                pod.getStatus().getContainerStatuses().stream().allMatch(ContainerStatus::getReady);
    }
}
