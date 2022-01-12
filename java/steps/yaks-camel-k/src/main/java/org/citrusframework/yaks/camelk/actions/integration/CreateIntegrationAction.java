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

package org.citrusframework.yaks.camelk.actions.integration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import com.consol.citrus.variable.VariableUtils;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.Integration;
import org.citrusframework.yaks.camelk.model.IntegrationList;
import org.citrusframework.yaks.camelk.model.IntegrationSpec;

/**
 * Test action creates new Camel-K integration with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type integration.
 *
 * @author Christoph Deppisch
 */
public class CreateIntegrationAction extends AbstractCamelKAction {

    private final String integrationName;
    private final String fileName;
    private final String source;
    private final List<String> dependencies;
    private final List<String> properties;
    private final List<String> propertyFiles;
    private final List<String> traits;
    private final Map<String, String> openApis;
    private final boolean supportVariables;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateIntegrationAction(Builder builder) {
        super("create-integration", builder);
        this.integrationName = builder.integrationName;
        this.fileName = builder.fileName;
        this.source = builder.source;
        this.dependencies = builder.dependencies;
        this.properties = builder.properties;
        this.propertyFiles = builder.propertyFiles;
        this.traits = builder.traits;
        this.openApis = builder.openApis;
        this.supportVariables = builder.supportVariables;
    }

    @Override
    public void doExecute(TestContext context) {
        createIntegration(context);
    }

    private void createIntegration(TestContext context) {
        String resolvedSource;
        if (supportVariables) {
            resolvedSource = context.replaceDynamicContentInString(source);
        } else {
            resolvedSource = source;
        }

        final Integration.Builder integrationBuilder = new Integration.Builder()
                .name(context.replaceDynamicContentInString(integrationName))
                .source(context.replaceDynamicContentInString(fileName), resolvedSource);

        List<String> resolvedDependencies = resolveDependencies(resolvedSource, context.resolveDynamicValuesInList(dependencies));
        if (!resolvedDependencies.isEmpty()) {
            integrationBuilder.dependencies(resolvedDependencies);
        }
        addPropertyConfigurationSpec(integrationBuilder, context);
        addTraitSpec(integrationBuilder, resolvedSource, context);
        addOpenApiSpec(integrationBuilder, context);

        final Integration i = integrationBuilder.build();
        CustomResourceDefinitionContext ctx = CamelKSupport.integrationCRDContext(CamelKSettings.getApiVersion());
        getKubernetesClient().customResources(ctx, Integration.class, IntegrationList.class)
                .inNamespace(CamelKSettings.getNamespace())
                .createOrReplace(i);

        LOG.info(String.format("Successfully created Camel-K integration '%s'", i.getMetadata().getName()));
    }

    private void addOpenApiSpec(Integration.Builder integrationBuilder, TestContext context) {
        openApis.forEach((k, v) -> integrationBuilder.openApi(k, context.replaceDynamicContentInString(v)));
    }

    private void addTraitSpec(Integration.Builder integrationBuilder, String source, TestContext context) {
        final Map<String, IntegrationSpec.TraitConfig> traitConfigMap = new HashMap<>();

        if (traits != null && !traits.isEmpty()) {
            for (String t : context.resolveDynamicValuesInList(traits)){
                addTraitSpec(t, traitConfigMap);
            }
        }

        Pattern pattern = Pattern.compile("^// camel-k: ?trait=(.+)$", Pattern.MULTILINE);
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            addTraitSpec(depMatcher.group(1), traitConfigMap);
        }

        if (!traitConfigMap.isEmpty()) {
            integrationBuilder.traits(traitConfigMap);
        }
    }

    private void addTraitSpec(String traitExpression, Map<String, IntegrationSpec.TraitConfig> configMap) {
        //traitName.key=value
        if (!validateTraitFormat(traitExpression)) {
            throw new IllegalArgumentException("Trait " + traitExpression + " does not match format traitName.key=value");
        }
        final String[] trait = traitExpression.split("\\.",2);
        final String[] traitConfig = trait[1].split("=", 2);

        final String traitKey = traitConfig[0];
        final Object traitValue = resolveTraitValue(traitKey, traitConfig[1].trim());
        if (configMap.containsKey(trait[0])) {
            configMap.get(trait[0]).add(traitKey, traitValue);
        } else {
            configMap.put(trait[0], new IntegrationSpec.TraitConfig(traitKey, traitValue));
        }
    }

    /**
     * Resolve trait value with automatic type conversion. Enabled trait keys need to be converted to boolean type.
     * @param traitKey
     * @param value
     * @return
     */
    private Object resolveTraitValue(String traitKey, String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return VariableUtils.cutOffDoubleQuotes(value);
        }

        if (value.startsWith("'") && value.endsWith("'")) {
            return VariableUtils.cutOffSingleQuotes(value);
        }

        if (traitKey.equalsIgnoreCase("enabled")) {
            return Boolean.valueOf(value);
        }

        return value;
    }

    private void addPropertyConfigurationSpec(Integration.Builder integrationBuilder, TestContext context) {
        final List<IntegrationSpec.Configuration> configurationList = new ArrayList<>();
        if (properties != null && !properties.isEmpty()) {
            for (String p : context.resolveDynamicValuesInList(properties)){
                //key=value
                if (!validatePropertyFormat(p)) {
                    throw new IllegalArgumentException("Property " + p + " does not match format key=value");
                }
                final String[] property = p.split("=",2);
                configurationList.add(
                        new IntegrationSpec.Configuration("property", createPropertySpec(property[0], property[1], context)));
            }
        }

        if (propertyFiles != null && !propertyFiles.isEmpty()) {
            for (String pf : propertyFiles){
                try {
                    Properties props = new Properties();
                    props.load(FileUtils.getFileResource(pf, context).getInputStream());
                    props.forEach((key, value) -> configurationList.add(
                            new IntegrationSpec.Configuration("property", createPropertySpec(key.toString(), value.toString(), context))));
                } catch (IOException e) {
                    throw new CitrusRuntimeException("Failed to load property file", e);
                }
            }
        }

        if (!configurationList.isEmpty()) {
            integrationBuilder.configuration(configurationList);
        }
    }

    private String createPropertySpec(String key, String value, TestContext context) {
        return escapePropertyItem(key) + "=" + escapePropertyItem(context.replaceDynamicContentInString(value));
    }

    private String escapePropertyItem(String item) {
        return item.replaceAll(":", "\\:").replaceAll("=", "\\=");
    }

    private boolean validateTraitFormat(String trait) {
        String patternString = "[A-Za-z-0-9]+\\.[A-Za-z-0-9]+=[A-Za-z-0-9]+";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(trait);
        return matcher.matches();
    }

    private boolean validatePropertyFormat(String property) {
        String patternString = "[^\\s]+=.*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(property);
        return matcher.matches();
    }

    /**
     * Resolve dependencies for Camel-K integration and support modeline instructions in given source.
     * @param source
     * @param dependencies
     * @return
     */
    private static List<String> resolveDependencies(String source, List<String> dependencies) {
        List<String> resolved = new ArrayList<>(dependencies);

        Pattern pattern = Pattern.compile("^// camel-k: ?dependency=(.+)$", Pattern.MULTILINE);
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            String dependency = depMatcher.group(1);

            if (dependency.startsWith("camel-quarkus-")) {
                dependency = "camel:" + dependency.substring("camel-quarkus-".length());
            } else if (dependency.startsWith("camel-quarkus:")) {
                dependency = "camel:" + dependency.substring("camel-quarkus:".length());
            } else if (dependency.startsWith("camel-")) {
                dependency = "camel:" + dependency.substring("camel-".length());
            }

            resolved.add(dependency);
        }

        return resolved;
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<CreateIntegrationAction, Builder> {

        private String integrationName;
        private String fileName;
        private String source;
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> properties = new ArrayList<>();
        private final List<String> propertyFiles = new ArrayList<>();
        private final List<String> traits = new ArrayList<>();
        private final Map<String, String> openApis = new LinkedHashMap<>();
        private boolean supportVariables = true;

        public Builder integration(String integrationName) {
            this.integrationName = integrationName;

            if (fileName == null) {
                fileName = integrationName;
            }

            return this;
        }

        public Builder supportVariables(boolean supportVariables) {
            this.supportVariables = supportVariables;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder source(String fileName, String source) {
            this.fileName = fileName;
            this.source = source;
            return this;
        }

        public Builder openApi(String fileName, String content) {
            this.openApis.put(fileName, content);
            return this;
        }

        public Builder dependencies(String dependencies) {
            if (dependencies != null && dependencies.length() > 0) {
                dependencies(Arrays.asList(dependencies.split(",")));
            }

            return this;
        }

        public Builder propertyFile(String propertyFile) {
            this.propertyFiles.add(propertyFile);
            return this;
        }

        public Builder propertyFiles(List<String> propertyFiles) {
            this.propertyFiles.addAll(propertyFiles);
            return this;
        }

        public Builder properties(String properties) {
            if (properties != null && properties.length() > 0) {
                properties(Arrays.asList(properties.split(",")));
            }

            return this;
        }

        public Builder properties(List<String> properties) {
            this.properties.addAll(properties);
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            properties.forEach(this::property);
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public Builder dependency(String dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder property(String name, String value) {
            this.properties.add(name + "=" + value);
            return this;
        }

        public Builder traits(String traits) {
            if (traits != null && traits.length() > 0) {
                traits(Arrays.asList(traits.split(",")));
            }

            return this;
        }

        public Builder traits(List<String> traits) {
            this.traits.addAll(traits);
            return this;
        }

        public Builder trait(String name, String key, Object value) {
            this.traits.add(String.format("%s.%s=%s", name, key, value));
            return this;
        }

        @Override
        public CreateIntegrationAction build() {
            return new CreateIntegrationAction(this);
        }
    }
}
