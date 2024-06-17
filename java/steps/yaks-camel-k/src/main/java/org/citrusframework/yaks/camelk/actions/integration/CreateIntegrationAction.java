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

package org.citrusframework.yaks.camelk.actions.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Updatable;
import org.apache.camel.v1.Integration;
import org.apache.camel.v1.IntegrationBuilder;
import org.apache.camel.v1.IntegrationSpecBuilder;
import org.apache.camel.v1.integrationspec.ConfigurationBuilder;
import org.apache.camel.v1.integrationspec.SourcesBuilder;
import org.apache.camel.v1.integrationspec.Traits;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.variable.VariableUtils;
import org.citrusframework.yaks.YaksSettings;
import org.citrusframework.yaks.camelk.actions.AbstractCamelKAction;
import org.citrusframework.yaks.camelk.jbang.CamelJBangSettings;
import org.citrusframework.yaks.camelk.jbang.ProcessAndOutput;
import org.citrusframework.yaks.camelk.model.IntegrationList;
import org.citrusframework.yaks.kubernetes.KubernetesSupport;
import org.citrusframework.yaks.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.citrusframework.yaks.camelk.jbang.CamelJBang.camel;

/**
 * Test action creates new Camel K integration with given name and source code. Uses given Kubernetes client to
 * create a custom resource of type integration.
 *
 * @author Christoph Deppisch
 */
public class CreateIntegrationAction extends AbstractCamelKAction {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CreateIntegrationAction.class);

    private final String integrationName;
    private final String fileName;
    private final String source;
    private final List<String> dependencies;
    private final List<String> buildProperties;
    private final List<String> buildPropertyFiles;
    private final List<String> envVars;
    private final List<String> envVarFiles;
    private final List<String> properties;
    private final List<String> propertyFiles;
    private final List<String> traits;
    private final List<String> openApis;
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
        this.buildProperties = builder.buildProperties;
        this.buildPropertyFiles = builder.buildPropertyFiles;
        this.envVars = builder.envVars;
        this.envVarFiles = builder.envVarFiles;
        this.properties = builder.properties;
        this.propertyFiles = builder.propertyFiles;
        this.traits = builder.traits;
        this.openApis = builder.openApis;
        this.supportVariables = builder.supportVariables;
    }

    @Override
    public void doExecute(TestContext context) {
        String name = context.replaceDynamicContentInString(integrationName);

        LOG.info(String.format("Creating Camel K integration '%s'", name));

        String resolvedSource;
        if (supportVariables) {
            resolvedSource = context.replaceDynamicContentInString(source);
        } else {
            resolvedSource = source;
        }

        final IntegrationBuilder integrationBuilder = new IntegrationBuilder()
                .withNewMetadata()
                    .withName(name)
                .endMetadata();

        IntegrationSpecBuilder specBuilder = new IntegrationSpecBuilder();
        specBuilder.addToSources(new SourcesBuilder().withName(context.replaceDynamicContentInString(fileName)).withContent(resolvedSource).build());

        List<String> resolvedDependencies = resolveDependencies(resolvedSource, context.resolveDynamicValuesInList(dependencies));
        if (!resolvedDependencies.isEmpty()) {
            specBuilder.addAllToDependencies(resolvedDependencies);
        }
        Map<String, Map<String, Object>> traitConfigMap = new HashMap<>();
        addPropertyConfigurationSpec(specBuilder, context);
        addRuntimeConfigurationSpec(specBuilder, resolvedSource, context);
        addBuildPropertyConfigurationSpec(traitConfigMap, resolvedSource, context);
        addEnvVarConfigurationSpec(traitConfigMap, resolvedSource, context);
        addOpenApiSpec(traitConfigMap, resolvedSource, context);
        addTraitSpec(traitConfigMap, resolvedSource, context);

        specBuilder.withTraits(KubernetesSupport.json().convertValue(traitConfigMap, Traits.class));

        final Integration integration = integrationBuilder
                .withSpec(specBuilder.build())
                .build();
        if (YaksSettings.isLocal(clusterType(context))) {
            createLocalIntegration(integration, integration.getMetadata().getName(), context);
        } else {
            createIntegration(getKubernetesClient(), namespace(context), integration);
        }

        LOG.info(String.format("Successfully created Camel K integration '%s'", integration.getMetadata().getName()));
    }

    /**
     * Creates the Camel K integration as a custom resource in given namespace.
     * @param k8sClient
     * @param namespace
     * @param integration
     */
    private static void createIntegration(KubernetesClient k8sClient, String namespace, Integration integration) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(KubernetesSupport.yaml(new IntegrationValuePropertyMapper()).dumpAsMap(integration));
        }

        k8sClient.resources(Integration.class, IntegrationList.class)
                .inNamespace(namespace)
                .resource(integration)
                .createOr(Updatable::update);
    }

    /**
     * Creates the Camel K integration with local JBang runtime.
     * @param integration
     * @param name
     * @param context
     */
    private static void createLocalIntegration(Integration integration, String name, TestContext context) {
        try {
            String integrationYaml = KubernetesSupport.yaml(new IntegrationValuePropertyMapper()).dumpAsMap(integration);

            if (LOG.isDebugEnabled()) {
                LOG.debug(integrationYaml);
            }

            // CAMEL-18802: Workaround for Camel JBang not supporting integration custom resources at the moment
            integrationYaml += "#KameletBinding";

            Path workDir = CamelJBangSettings.getWorkDir();
            Files.createDirectories(workDir);
            Path file = workDir.resolve(String.format("i-%s.yaml", name));
            Files.writeString(file, integrationYaml,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            ProcessAndOutput pao = camel().run(name, file, camelRunArgs(integration));

            if (!pao.getProcess().isAlive()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(pao.getOutput());
                }

                throw new CitrusRuntimeException(String.format("Failed to create Camel K integration - exit code %s", pao.getProcess().exitValue()));
            }

            Long pid = pao.getCamelProcessId();
            context.setVariable(name + ":pid", pid);
            context.setVariable(name + ":process:" + pid, pao);
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to create integration file", e);
        }
    }

    /**
     * Construct optional Camel JBang run command args from given integration.
     * @param integration
     * @return
     */
    private static String[] camelRunArgs(Integration integration) {
        List<String> args = new ArrayList<>();

        if (integration.getSpec().getTraits().getOpenapi() != null) {
            for (String openApi : integration.getSpec().getTraits().getOpenapi().getConfigmaps()) {
                args.add("--open-api");
                if (openApi.startsWith("configmap:")) {
                    args.add(openApi.substring("configmap:".length()));
                } else {
                    args.add(openApi);
                }
            }
        }

        return args.toArray(String[]::new);
    }

    private void addOpenApiSpec(Map<String, Map<String, Object>> traitConfigMap, String source, TestContext context) {
        String traitName = "openapi.configmaps";
        Pattern pattern = getModelinePattern("open-api");
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            String openApiSpecFile = depMatcher.group(1);
            addTraitSpec("%s=%s".formatted(traitName, context.replaceDynamicContentInString(openApiSpecFile)), traitConfigMap);
        }

        for (String openApi : openApis) {
            addTraitSpec("%s=%s".formatted(traitName, context.replaceDynamicContentInString(openApi)), traitConfigMap);
        }
    }

    private void addTraitSpec(Map<String, Map<String, Object>> traitConfigMap, String source, TestContext context) {
        if (traits != null && !traits.isEmpty()) {
            for (String t : context.resolveDynamicValuesInList(traits)) {
                addTraitSpec(t, traitConfigMap);
            }
        }

        Pattern pattern = getModelinePattern("trait");
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            addTraitSpec(depMatcher.group(1), traitConfigMap);
        }
    }

    private void addTraitSpec(String traitExpression, Map<String, Map<String, Object>> traitConfigMap) {
        //traitName.key=value
        final String[] trait = traitExpression.split("\\.",2);
        final String[] traitConfig = trait[1].split("=", 2);

        final String traitKey = traitConfig[0];
        final Object traitValue = resolveTraitValue(traitKey, traitConfig[1].trim());
        if (traitConfigMap.containsKey(trait[0])) {
            Map<String, Object> config = traitConfigMap.get(trait[0]);

            if (config.containsKey(traitKey)) {
                Object existingValue = config.get(traitKey);

                if (existingValue instanceof List) {
                    List<String> values = (List<String>) existingValue;
                    values.add(traitValue.toString());
                } else {
                    config.put(traitKey, Arrays.asList(existingValue.toString(), traitValue));
                }
            } else {
                config.put(traitKey, initializeTraitValue(traitValue));
            }
        } else {
            Map<String, Object> config = new HashMap<>();
            config.put(traitKey, initializeTraitValue(traitValue));
            traitConfigMap.put(trait[0], config);
        }
    }

    private Object initializeTraitValue(Object value) {
        if (value instanceof String && value.toString().startsWith("[") && value.toString().endsWith("]")) {
            List<String> values = new ArrayList<>();
            values.add(resolveTraitValue("", value.toString().substring(1, value.toString().length() - 1)).toString());
            return values;
        }

        return value;
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

        if (traitKey.equalsIgnoreCase("enabled") ||
                traitKey.equalsIgnoreCase("verbose")) {
            return Boolean.valueOf(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private void addPropertyConfigurationSpec(IntegrationSpecBuilder specBuilder, TestContext context) {
        if (properties != null && !properties.isEmpty()) {
            for (String p : context.resolveDynamicValuesInList(properties)){
                //key=value
                if (isValidPropertyFormat(p)) {
                    final String[] property = p.split("=",2);
                    specBuilder.addToConfiguration(
                            new ConfigurationBuilder().withType("property").withValue(createPropertySpec(property[0], property[1], context)).build());
                } else {
                    throw new IllegalArgumentException("Property " + p + " does not match format key=value");
                }
            }
        }

        if (propertyFiles != null && !propertyFiles.isEmpty()) {
            for (String pf : propertyFiles){
                try {
                    Properties props = new Properties();
                    props.load(ResourceUtils.resolve(pf, context).getInputStream());
                    props.forEach((key, value) -> specBuilder.addToConfiguration(
                            new ConfigurationBuilder().withType("property").withValue(createPropertySpec(key.toString(), value.toString(), context)).build()));
                } catch (IOException e) {
                    throw new CitrusRuntimeException("Failed to load property file", e);
                }
            }
        }
    }

    private void addBuildPropertyConfigurationSpec(Map<String, Map<String, Object>> traitConfigMap, String source, TestContext context) {
        final String traitName = "builder.properties";

        if (buildProperties != null && !buildProperties.isEmpty()) {
            for (String p : context.resolveDynamicValuesInList(buildProperties)){
                //key=value
                if (isValidPropertyFormat(p)) {
                    final String[] property = p.split("=", 2);
                    addTraitSpec(String.format("%s=%s", traitName, createPropertySpec(property[0], property[1], context)), traitConfigMap);
                } else {
                    throw new IllegalArgumentException("Property " + p + " does not match format key=value");
                }
            }
        }

        if (buildPropertyFiles != null && !buildPropertyFiles.isEmpty()) {
            for (String pf : buildPropertyFiles){
                try {
                    Properties props = new Properties();
                    props.load(ResourceUtils.resolve(pf, context).getInputStream());
                    props.forEach((key, value) -> addTraitSpec(String.format("%s=%s",
                            traitName,
                            createPropertySpec(key.toString(), value.toString(), context)), traitConfigMap));
                } catch (IOException e) {
                    throw new CitrusRuntimeException("Failed to load property file", e);
                }
            }
        }

        Pattern pattern = getModelinePattern("build-property");
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            addTraitSpec(String.format("%s=%s", traitName, depMatcher.group(1)), traitConfigMap);
        }
    }

    private void addEnvVarConfigurationSpec(Map<String, Map<String, Object>> traitConfigMap, String source, TestContext context) {
        final String traitName = "environment.vars";

        if (envVars != null && !envVars.isEmpty()) {
            for (String v : context.resolveDynamicValuesInList(envVars)){
                //key=value
                if (isValidPropertyFormat(v)) {
                    final String[] property = v.split("=", 2);
                    addTraitSpec(String.format("%s=%s", traitName, createPropertySpec(property[0], property[1], context)), traitConfigMap);
                } else {
                    throw new IllegalArgumentException("EnvVar " + v + " does not match format key=value");
                }
            }
        }

        if (envVarFiles != null && !envVarFiles.isEmpty()) {
            for (String vf : envVarFiles){
                try {
                    Properties props = new Properties();
                    props.load(ResourceUtils.resolve(vf, context).getInputStream());
                    props.forEach((key, value) -> addTraitSpec(String.format("%s=%s",
                            traitName,
                            createPropertySpec(key.toString(), value.toString(), context)), traitConfigMap));
                } catch (IOException e) {
                    throw new CitrusRuntimeException("Failed to load env var file", e);
                }
            }
        }

        Pattern pattern = getModelinePattern("env");
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            addTraitSpec(String.format("%s=%s", traitName, depMatcher.group(1)), traitConfigMap);
        }
    }

    private void addRuntimeConfigurationSpec(IntegrationSpecBuilder specBuilder, String source, TestContext context) {
        Pattern pattern = getModelinePattern("config");
        Matcher depMatcher = pattern.matcher(source);
        while (depMatcher.find()) {
            String[] config = depMatcher.group(1).split(":", 2);
            if (config.length == 2) {
                specBuilder.addToConfiguration(new ConfigurationBuilder().withType(config[0]).withValue(context.replaceDynamicContentInString(config[1])).build());
            } else {
                specBuilder.addToConfiguration(new ConfigurationBuilder().withType("property").withValue(context.replaceDynamicContentInString(depMatcher.group(1))).build());
            }
        }
    }

    private String createPropertySpec(String key, String value, TestContext context) {
        return escapePropertyItem(key) + "=" + escapePropertyItem(context.replaceDynamicContentInString(value));
    }

    private String escapePropertyItem(String item) {
        return item.replaceAll(":", "\\:").replaceAll("=", "\\=");
    }

    /**
     * Verify property expression format key=value
     * @param property
     * @return
     */
    private static boolean isValidPropertyFormat(String property) {
        String patternString = "[^\\s]+=.*";

        Pattern pattern = Pattern.compile(patternString);

        Matcher matcher = pattern.matcher(property);
        return matcher.matches();
    }

    /**
     * Resolve dependencies for Camel K integration and support modeline instructions in given source.
     * @param source
     * @param dependencies
     * @return
     */
    private static List<String> resolveDependencies(String source, List<String> dependencies) {
        List<String> resolved = new ArrayList<>(dependencies);

        Pattern pattern = getModelinePattern("dependency");
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
     * Create regexp pattern to match Camel K modeling instruction with given name.
     * @param name
     * @return
     */
    private static Pattern getModelinePattern(String name) {
        return Pattern.compile(String.format("^// camel-k: ?%s=(.+)$", name), Pattern.MULTILINE);
    }

    /**
     * Action builder.
     */
    public static final class Builder extends AbstractCamelKAction.Builder<CreateIntegrationAction, Builder> {

        private String integrationName;
        private String fileName;
        private String source;
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> buildProperties = new ArrayList<>();
        private final List<String> buildPropertyFiles = new ArrayList<>();
        private final List<String> envVars = new ArrayList<>();
        private final List<String> envVarFiles = new ArrayList<>();
        private final List<String> properties = new ArrayList<>();
        private final List<String> propertyFiles = new ArrayList<>();
        private final List<String> traits = new ArrayList<>();
        private final List<String> openApis = new ArrayList<>();
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

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder source(String fileName, String source) {
            this.fileName = fileName;
            this.source = source;
            return this;
        }

        public Builder openApi(String configMap) {
            this.openApis.add(configMap);
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

        public Builder buildPropertyFile(String propertyFile) {
            this.buildPropertyFiles.add(propertyFile);
            return this;
        }

        public Builder buildPropertyFiles(List<String> propertyFiles) {
            this.buildPropertyFiles.addAll(propertyFiles);
            return this;
        }

        public Builder buildProperties(String properties) {
            if (properties != null && !properties.isEmpty()) {
                buildProperties(Arrays.asList(properties.split(",")));
            }

            return this;
        }

        public Builder buildProperties(List<String> properties) {
            this.buildProperties.addAll(properties);
            return this;
        }

        public Builder buildProperties(Map<String, String> properties) {
            properties.forEach(this::buildProperty);
            return this;
        }

        public Builder envVarFile(String envVarFile) {
            this.envVarFiles.add(envVarFile);
            return this;
        }

        public Builder envVarFiles(List<String> envVarFiles) {
            this.envVarFiles.addAll(envVarFiles);
            return this;
        }

        public Builder envVars(String vars) {
            if (vars != null && !vars.isEmpty()) {
                envVars(Arrays.asList(vars.split(",")));
            }

            return this;
        }

        public Builder envVars(List<String> vars) {
            this.envVars.addAll(vars);
            return this;
        }

        public Builder envVars(Map<String, String> vars) {
            vars.forEach(this::envVar);
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

        public Builder buildProperty(String name, String value) {
            this.buildProperties.add(name + "=" + value);
            return this;
        }

        public Builder envVar(String name, String value) {
            this.envVars.add(name + "=" + value);
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
