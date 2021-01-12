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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import org.citrusframework.yaks.camelk.CamelKSettings;
import org.citrusframework.yaks.camelk.CamelKSupport;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.model.DoneableIntegration;
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
    private final String source;
    private final List<String> dependencies;
    private final List<String> properties;
    private final List<String> propertyFiles;
    private final String traits;

    /**
     * Constructor using given builder.
     * @param builder
     */
    public CreateIntegrationAction(Builder builder) {
        super("create-integration", builder);
        this.integrationName = builder.integrationName;
        this.source = builder.source;
        this.dependencies = builder.dependencies;
        this.properties = builder.properties;
        this.propertyFiles = builder.propertyFiles;
        this.traits = builder.traits;
    }

    @Override
    public void doExecute(TestContext context) {
        createIntegration(context);
    }

    private void createIntegration(TestContext context) {
        final Integration.Builder integrationBuilder = new Integration.Builder()
                .name(context.replaceDynamicContentInString(integrationName))
                .source(context.replaceDynamicContentInString(source));

        if (dependencies != null && !dependencies.isEmpty()) {
            integrationBuilder.dependencies(context.resolveDynamicValuesInList(dependencies));
        }

        addPropertyConfigurationSpec(integrationBuilder, context);
        addTraitSpec(integrationBuilder, context);

        final Integration i = integrationBuilder.build();
        CustomResourceDefinitionContext ctx = CamelKSupport.integrationCRDContext(CamelKSettings.getApiVersion());
        getKubernetesClient().customResources(ctx, Integration.class, IntegrationList.class, DoneableIntegration.class)
                .inNamespace(CamelKSettings.getNamespace())
                .createOrReplace(i);

        LOG.info(String.format("Successfully created Camel-K integration '%s'", i.getMetadata().getName()));
    }

    private void addTraitSpec(Integration.Builder integrationBuilder, TestContext context) {
        if (traits != null && !traits.isEmpty()) {
            final Map<String, IntegrationSpec.TraitConfig> traitConfigMap = new HashMap<>();
            for (String t : context.replaceDynamicContentInString(traits).split(",")){
                //traitName.key=value
                if (!validateTraitFormat(t)) {
                    throw new IllegalArgumentException("Trait " + t + " does not match format traitName.key=value");
                }
                final String[] trait = t.split("\\.",2);
                final String[] traitConfig = trait[1].split("=", 2);
                if (traitConfigMap.containsKey(trait[0])) {
                    traitConfigMap.get(trait[0]).add(traitConfig[0], traitConfig[1]);
                } else {
                    traitConfigMap.put(trait[0], new IntegrationSpec.TraitConfig(traitConfig[0], traitConfig[1]));
                }
            }
            integrationBuilder.traits(traitConfigMap);
        }
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
                        new IntegrationSpec.Configuration("property", createPropertySpec(property[0], property[1])));
            }
        }

        if (propertyFiles != null && !propertyFiles.isEmpty()) {
            for (String pf : propertyFiles){
                try {
                    Properties props = new Properties();
                    props.load(FileUtils.getFileResource(pf, context).getInputStream());
                    props.forEach((key, value) -> configurationList.add(
                            new IntegrationSpec.Configuration("property", createPropertySpec(key.toString(), value.toString()))));
                } catch (IOException e) {
                    throw new CitrusRuntimeException("Failed to load property file", e);
                }
            }
        }

        if (!configurationList.isEmpty()) {
            integrationBuilder.configuration(configurationList);
        }
    }

    private String createPropertySpec(String key, String value) {
        return escapePropertyItem(key) + "=" + escapePropertyItem(value);
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
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<CreateIntegrationAction, Builder> {

        private String integrationName;
        private String source;
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> properties = new ArrayList<>();
        private final List<String> propertyFiles = new ArrayList<>();
        private String traits;

        public Builder integration(String integrationName) {
            this.integrationName = integrationName;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder dependencies(String dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies.split(",")));
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
            this.properties.addAll(Arrays.asList(properties.split(",")));
            return this;
        }

        public Builder properties(List<String> properties) {
            this.properties.addAll(properties);
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
            this.traits = traits;
            return this;
        }

        @Override
        public CreateIntegrationAction build() {
            return new CreateIntegrationAction(this);
        }
    }
}
