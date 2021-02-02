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

package org.citrusframework.yaks.camel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.camel.endpoint.CamelEndpoint;
import com.consol.citrus.camel.endpoint.CamelEndpointConfiguration;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.util.FileUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spi.XMLRoutesDefinitionLoader;
import org.apache.camel.spring.SpringCamelContext;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;
import static com.consol.citrus.camel.actions.CamelRouteActionBuilder.camel;

public class CamelSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    @CitrusResource
    private TestContext context;

    private CamelContext camelContext;

    private String contextName = CamelSettings.getContextName();

    private Map<String, CamelEndpoint> endpoints = new HashMap<>();

    private Map<String, Object> headers = new HashMap<>();
    private String body;

    private long timeout = CamelSettings.getTimeout();

    private boolean globalCamelContext = false;
    private boolean autoRemoveResources = CamelSettings.isAutoRemoveResources();

    @Before
    public void before(Scenario scenario) {
        if (camelContext == null) {
            if (citrus.getCitrusContext().getReferenceResolver().resolveAll(CamelContext.class).size() == 1L) {
                camelContext = citrus.getCitrusContext().getReferenceResolver().resolve(CamelContext.class);
                globalCamelContext = true;
            } else if (citrus.getCitrusContext().getReferenceResolver().isResolvable(contextName)) {
                camelContext = citrus.getCitrusContext().getReferenceResolver().resolve(contextName, CamelContext.class);
                globalCamelContext = true;
            } else {
                camelContext();
            }
        }

        headers = new HashMap<>();
        body = null;
    }

    @After
    public void after(Scenario scenario) {
        if (autoRemoveResources) {
            endpoints.clear();
            destroyCamelContext();
        }
    }

    @Given("^Disable auto removal of Camel resources$")
    public void disableAutoRemove() {
        autoRemoveResources = false;
    }

    @Given("^Enable auto removal of Camel resources$")
    public void enableAutoRemove() {
        autoRemoveResources = true;
    }

    @Given("^(?:Default|New) Camel context$")
    public void defaultContext() {
        destroyCamelContext();
        camelContext();
        globalCamelContext = false;
    }

    @Given("^New Spring Camel context$")
    public void camelContext(String beans) {
        destroyCamelContext();

        try {
            ApplicationContext ctx = new GenericXmlApplicationContext(
                    new ByteArrayResource(context.replaceDynamicContentInString(beans).getBytes(StandardCharsets.UTF_8)));
            camelContext = ctx.getBean(SpringCamelContext.class);
            camelContext.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Spring Camel context", e);
        }
        globalCamelContext = false;
    }

    @Given("^Camel consumer timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Given("^Camel route ([^\\s]+)\\.xml")
    public void camelRouteXml(String id, String routeSpec) throws Exception {
        String routeXml;

        if (routeSpec.startsWith("<route>")) {
            routeXml = String.format("<route id=\"%s\" xmlns=\"http://camel.apache.org/schema/spring\">", id) + routeSpec.substring("<route>".length());
        } else if (routeSpec.startsWith("<route")) {
            routeXml = routeSpec;
        } else {
            routeXml = String.format("<route id=\"%s\" xmlns=\"http://camel.apache.org/schema/spring\">", id) + routeSpec + "</route>";
        }

        final XMLRoutesDefinitionLoader loader = camelContext().adapt(ExtendedCamelContext.class).getXMLRoutesDefinitionLoader();
        Object result = loader.loadRoutesDefinition(camelContext(),
                new ByteArrayInputStream(context.replaceDynamicContentInString(routeXml).getBytes(StandardCharsets.UTF_8)));
        if (result instanceof RoutesDefinition) {
            RoutesDefinition routeDefinition = (RoutesDefinition) result;

            camelContext().addRoutes(new RouteBuilder(camelContext()) {
                @Override
                public void configure() throws Exception {
                    for (RouteDefinition route : routeDefinition.getRoutes()) {
                        try {
                            getRouteCollection().getRoutes().add(route);
                            log.info(String.format("Created new Camel route '%s' in context '%s'", route.getRouteId(), camelContext().getName()));
                        } catch (Exception e) {
                            throw new CitrusRuntimeException(String.format("Failed to create route definition '%s' in context '%s'", route.getRouteId(), camelContext.getName()), e);
                        }
                    }
                }
            });

            for (RouteDefinition route : routeDefinition.getRoutes()) {
                camelContext().adapt(AbstractCamelContext.class).startRoute(route.getRouteId());
            }
        }
    }

    @Given("^Camel route ([^\\s]+)\\.groovy")
    public void camelRouteGroovy(String id, String route) throws Exception {
        RouteBuilder routeBuilder = new RouteBuilder(camelContext()) {
            @Override
            public void configure() throws Exception {
                ImportCustomizer ic = new ImportCustomizer();
                ic.addStarImports("org.apache.camel");

                CompilerConfiguration cc = new CompilerConfiguration();
                cc.addCompilationCustomizers(ic);
                cc.setScriptBaseClass(DelegatingScript.class.getName());

                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                GroovyShell sh = new GroovyShell(cl, new Binding(), cc);

                DelegatingScript script = (DelegatingScript) sh.parse(context.replaceDynamicContentInString(route));

                // set the delegate target
                script.setDelegate(this);
                script.run();
            }

            @Override
            protected void configureRoute(RouteDefinition route) {
                route.routeId(id);
            }
        };

        camelContext().addRoutes(routeBuilder);
    }

    @Given("^start Camel route ([^\\s]+)$")
    public void startRoute(String routeId) {
        runner.run(camel().context(camelContext().adapt(ModelCamelContext.class))
                                     .start(routeId));
    }

    @Given("^stop Camel route ([^\\s]+)$")
    public void stopRoute(String routeId) {
        runner.run(camel().context(camelContext().adapt(ModelCamelContext.class))
                                     .stop(routeId));
    }

    @Given("^remove Camel route ([^\\s]+)$")
    public void removeRoute(String routeId) {
        runner.run(camel().context(camelContext().adapt(ModelCamelContext.class))
                                     .remove(routeId));
    }

    @Given("^Camel exchange message header ([^\\s]+)(?:=| is )\"(.+)\"$")
    @Then("^(?:expect|verify) Camel exchange message header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addMessageHeader(String name, Object value) {
        headers.put(name, value);
    }

    @Given("^Camel exchange message headers$")
    public void addMessageHeaders(DataTable headers) {
        Map<String, Object> headerPairs = headers.asMap(String.class, Object.class);
        headerPairs.forEach(this::addMessageHeader);
    }

    @Given("^Camel exchange body$")
    @Then("^(?:expect|verify) Camel exchange body$")
    public void setExchangeBodyMultiline(String body) {
        setExchangeBody(body);
    }

    @Given("^Camel exchange body: (.+)$")
    @Then("^(?:expect|verify) Camel exchange body: (.+)$")
    public void setExchangeBody(String body) {
        this.body = body;
    }

    @Given("^load Camel exchange body ([^\\s]+)$")
    @Then("^(?:expect|verify) Camel exchange body loaded from ([^\\s]+)$")
    public void loadExchangeBody(String file) {
        try {
            this.body = FileUtils.readToString(FileUtils.getFileResource(file));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load body from file resource %s", file));
        }
    }

    @When("^send Camel exchange to\\(\"(.+)\"\\)$")
    public void sendExchange(String endpointUri) {
        runner.run(send().endpoint(camelEndpoint(endpointUri))
                .payload(body)
                .headers(headers));

        body = null;
        headers.clear();
    }

    @Then("^receive Camel exchange from\\(\"(.+)\"\\)$")
    public void receiveExchange(String endpointUri) {
        runner.run(receive().endpoint(camelEndpoint(endpointUri))
                .timeout(timeout)
                .payload(body)
                .headers(headers));

        body = null;
        headers.clear();
    }

    @When("^send Camel exchange to\\(\"(.+)\"\\) with body$")
    public void sendExchangeMultilineBody(String endpointUri, String body) {
        setExchangeBody(body);
        sendExchange(endpointUri);
    }

    @When("^send Camel exchange to\\(\"(.+)\"\\) with body: (.+)$")
    public void sendExchangeBody(String endpointUri, String body) {
        setExchangeBody(body);
        sendExchange(endpointUri);
    }

    @When("^send Camel exchange to\\(\"(.+)\"\\) with body and headers: (.+)$")
    public void sendMessageBodyAndHeaders(String endpointUri, String body, DataTable headers) {
        setExchangeBody(body);
        addMessageHeaders(headers);
        sendExchange(endpointUri);
    }

    @Then("^(?:receive|expect|verify) Camel exchange from\\(\"(.+)\"\\) with body$")
    public void receiveExchangeBodyMultiline(String endpointUri, String body) {
        setExchangeBody(body);
        receiveExchange(endpointUri);
    }
    @Then("^(?:receive|expect|verify) Camel exchange from\\(\"(.+)\"\\) with body: (.+)$")
    public void receiveExchangeBody(String endpointUri, String body) {
        setExchangeBody(body);
        receiveExchange(endpointUri);
    }

    @Then("^(?:receive|expect|verify) Camel exchange from\\(\"(.+)\"\\) message with body and headers: (.+)$")
    public void receiveFromJms(String endpointUri, String body, DataTable headers) {
        setExchangeBody(body);
        addMessageHeaders(headers);
        receiveExchange(endpointUri);
    }

    // **************************
    // Helpers
    // **************************

    private CamelEndpoint camelEndpoint(String endpointUri) {
        if (endpoints.containsKey(endpointUri)) {
            return endpoints.get(endpointUri);
        }

        CamelEndpointConfiguration endpointConfiguration = new CamelEndpointConfiguration();
        endpointConfiguration.setCamelContext(camelContext());
        endpointConfiguration.setEndpointUri(endpointUri);
        endpointConfiguration.setTimeout(timeout);
        CamelEndpoint endpoint = new CamelEndpoint(endpointConfiguration);

        endpoints.put(endpointUri, endpoint);

        return endpoint;
    }

    private CamelContext camelContext() {
        if (camelContext == null) {
            try {
                camelContext = new DefaultCamelContext();
                camelContext.start();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start default Camel context", e);
            }
        }

        return camelContext;
    }

    private void destroyCamelContext() {
        if (globalCamelContext) {
            // do not destroy global Camel context
            return;
        }

        try {
            if (camelContext != null) {
                camelContext.stop();
                camelContext = null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stop existing Camel context", e);
        }
    }
}
