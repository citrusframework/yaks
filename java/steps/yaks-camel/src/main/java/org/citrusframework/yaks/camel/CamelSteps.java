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
import java.nio.charset.StandardCharsets;

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.camel.endpoint.CamelEndpoint;
import com.consol.citrus.camel.endpoint.CamelEndpointConfiguration;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;
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

    private CamelContext camelContext;

    private String requestBody;
    private String responseBody;

    @Given("^(?:Default|New) Camel context$")
    public void defaultContext() {
        destroyCamelContext();
        camelContext();
    }

    @Given("^New Spring Camel context$")
    public void camelContext(String beans) {
        destroyCamelContext();

        try {
            ApplicationContext ctx = new GenericXmlApplicationContext(new ByteArrayResource(beans.getBytes(StandardCharsets.UTF_8)));
            camelContext = ctx.getBean(SpringCamelContext.class);
            camelContext.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Spring Camel context", e);
        }
    }

    @Given("^Camel route ([^\"\\s]+)\\.xml")
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
        Object result = loader.loadRoutesDefinition(camelContext(), new ByteArrayInputStream(routeXml.getBytes(StandardCharsets.UTF_8)));
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

    @Given("^Camel route ([^\"\\s]+)\\.groovy")
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

                 DelegatingScript script = (DelegatingScript) sh.parse(route);

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

    @Given("^start route (.+)$")
    public void startRoute(String routeId) {
        runner.run(camel().context(camelContext().adapt(ModelCamelContext.class))
                                     .start(routeId));
    }

    @Given("^stop route (.+)$")
    public void stopRoute(String routeId) {
        runner.run(camel().context(camelContext().adapt(ModelCamelContext.class))
                                     .stop(routeId));
    }

    @Given("^remove route (.+)$")
    public void removeRoute(String routeId) {
        runner.run(camel().context(camelContext().adapt(ModelCamelContext.class))
                                     .remove(routeId));
    }

    @Given("^request body$")
    public void setRequestBodyMultiline(String body) {
        setRequestBody(body);
    }

    @Given("^request body: (.+)$")
    public void setRequestBody(String body) {
        this.requestBody = body;
    }

    @When("^send to route ([^\"\\s]+)$")
    public void sendExchange(String endpointUri) {
        runner.run(send().endpoint(camelEndpoint(endpointUri))
                                    .payload(requestBody));
    }

    @When("^send to route ([^\"\\s]+) body$")
    public void sendExchangeMultilineBody(String endpointUri, String body) {
        sendExchangeBody(endpointUri, body);
    }

    @When("^send to route ([^\"\\s]+) body: (.+)$")
    public void sendExchangeBody(String endpointUri, String body) {
        runner.run(send().endpoint(camelEndpoint(endpointUri))
                                    .payload(body));
    }

    @Then("^(?:expect|verify) body received$")
    public void setResponseBodyMultiline(String body) {
        setResponseBody(body);
    }

    @Then("^(?:expect|verify) body received: (.+)$")
    public void setResponseBody(String body) {
        this.responseBody = body;
    }

    @Then("^receive from route ([^\"\\s]+)$")
    public void receiveExchange(String endpointUri) {
        runner.run(receive().endpoint(camelEndpoint(endpointUri))
                                       .payload(responseBody));
    }

    @Then("^receive from route ([^\"\\s]+) body$")
    public void receiveExchangeBodyMultiline(String endpointUri, String body) {
        receiveExchangeBody(endpointUri, body);
    }
    @Then("^receive from route ([^\"\\s]+) body: (.+)$")
    public void receiveExchangeBody(String endpointUri, String body) {
        runner.run(receive().endpoint(camelEndpoint(endpointUri))
                                       .payload(body));
    }

    // **************************
    // Helpers
    // **************************

    private CamelEndpoint camelEndpoint(String endpointUri) {
        CamelEndpointConfiguration endpointConfiguration = new CamelEndpointConfiguration();
        endpointConfiguration.setCamelContext(camelContext());
        endpointConfiguration.setEndpointUri(endpointUri);
        return new CamelEndpoint(endpointConfiguration);
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
