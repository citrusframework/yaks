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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.consol.citrus.Citrus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.yaml.snakeyaml.DumperOptions;
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
        OBJECT_MAPPER = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                .enable(MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                .build()
                .setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY));
    }

    private KubernetesSupport() {
        // prevent instantiation of utility class
    }

    public static KubernetesClient getKubernetesClient(Citrus citrus) {
        if (citrus.getCitrusContext().getReferenceResolver().resolveAll(KubernetesClient.class).size() == 1L) {
            return citrus.getCitrusContext().getReferenceResolver().resolve(KubernetesClient.class);
        } else {
            return new KubernetesClientBuilder().build();
        }
    }

    public static Yaml yaml() {
        Representer representer = new Representer(new DumperOptions()) {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                // if value of property is null, ignore it.
                if (propertyValue == null || (propertyValue instanceof Collection && ((Collection<?>) propertyValue).isEmpty()) ||
                    (propertyValue instanceof Map && ((Map<?, ?>) propertyValue).isEmpty())) {
                    return null;
                } else {
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

    public static GenericKubernetesResource getResource(KubernetesClient k8sClient, String namespace,
                                                        CustomResourceDefinitionContext context, String resourceName) {
        return k8sClient.genericKubernetesResources(context.getGroup() + "/" + context.getVersion(), context.getKind()).inNamespace(namespace)
                .withName(resourceName)
                .get();
    }

    public static GenericKubernetesResourceList getResources(KubernetesClient k8sClient, String namespace,
                                                             CustomResourceDefinitionContext context) {
        return k8sClient.genericKubernetesResources(context.getGroup() + "/" + context.getVersion(), context.getKind())
                .inNamespace(namespace)
                .list();
    }

    public static GenericKubernetesResourceList getResources(KubernetesClient k8sClient, String namespace,
                                                             CustomResourceDefinitionContext context, String labelKey, String labelValue) {
        return k8sClient.genericKubernetesResources(context.getGroup() + "/" + context.getVersion(), context.getKind())
                .inNamespace(namespace)
                .withLabel(labelKey, labelValue)
                .list();
    }

    public static <T> void createResource(KubernetesClient k8sClient, String namespace,
                                   CustomResourceDefinitionContext context, T resource) {
        createResource(k8sClient, namespace, context, yaml().dumpAsMap(resource));
    }

    public static void createResource(KubernetesClient k8sClient, String namespace,
                                   CustomResourceDefinitionContext context, String yaml) {
        k8sClient.genericKubernetesResources(context).inNamespace(namespace)
                .load(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))).createOrReplace();
    }

    public static void deleteResource(KubernetesClient k8sClient, String namespace,
                                      CustomResourceDefinitionContext context, String resourceName) {
        k8sClient.genericKubernetesResources(context).inNamespace(namespace).withName(resourceName).delete();
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

    /**
     * Try to get the cluster IP address of given service.
     * Resolves service by its name in given namespace and retrieves the cluster IP setting from the service spec.
     * Returns empty Optional in case of errors or no cluster IP setting.
     * @param citrus
     * @param serviceName
     * @param namespace
     * @return
     */
    public static Optional<String> getServiceClusterIp(Citrus citrus, String serviceName, String namespace) {
        try {
            Service service = getKubernetesClient(citrus).services().inNamespace(namespace).withName(serviceName).get();
            if (service != null) {
                return Optional.ofNullable(service.getSpec().getClusterIP());
            }
        } catch (KubernetesClientException e) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
