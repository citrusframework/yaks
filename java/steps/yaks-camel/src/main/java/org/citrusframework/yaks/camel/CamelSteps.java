package org.citrusframework.yaks.camel;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.consol.citrus.Citrus;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.camel.endpoint.CamelEndpoint;
import com.consol.citrus.camel.endpoint.CamelEndpointConfiguration;
import com.consol.citrus.dsl.runner.TestRunner;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.SpringCamelContext;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;

public class CamelSteps {

    @CitrusResource
    private TestRunner runner;

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
    public void camelRouteXml(String id, String route) throws Exception {
        String routeXml;

        if (route.startsWith("<route>")) {
            routeXml = String.format("<route id=\"%s\" xmlns=\"http://camel.apache.org/schema/spring\">", id) + route.substring("<route>".length());
        } else if (route.startsWith("<route")) {
            routeXml = route;
        } else {
            routeXml = String.format("<route id=\"%s\" xmlns=\"http://camel.apache.org/schema/spring\">", id) + route + "</route>";
        }

        RoutesDefinition routeDefinition = ModelHelper.loadRoutesDefinition(camelContext(), new ByteArrayInputStream(routeXml.getBytes(StandardCharsets.UTF_8)));

        for (RouteDefinition definition : routeDefinition.getRoutes()) {
            camelContext().addRouteDefinition(definition);
            camelContext().startRoute(definition.getId());
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
                route.id(id);
            }
        };

        camelContext().addRoutes(routeBuilder);
    }

    @Given("^start route (.+)$")
    public void startRoute(String routeId) {
        runner.camel(action -> action.context(camelContext().adapt(ModelCamelContext.class))
                                     .start(routeId));
    }

    @Given("^stop route (.+)$")
    public void stopRoute(String routeId) {
        runner.camel(action -> action.context(camelContext().adapt(ModelCamelContext.class))
                                     .stop(routeId));
    }

    @Given("^remove route (.+)$")
    public void removeRoute(String routeId) {
        runner.camel(action -> action.context(camelContext().adapt(ModelCamelContext.class))
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
        runner.send(action -> action.endpoint(camelEndpoint(endpointUri))
                                    .payload(requestBody));
    }

    @When("^send to route ([^\"\\s]+) body$")
    public void sendExchangeMultilineBody(String endpointUri, String body) {
        sendExchangeBody(endpointUri, body);
    }

    @When("^send to route ([^\"\\s]+) body: (.+)$")
    public void sendExchangeBody(String endpointUri, String body) {
        runner.send(action -> action.endpoint(camelEndpoint(endpointUri))
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
        runner.receive(action -> action.endpoint(camelEndpoint(endpointUri))
                                       .payload(responseBody));
    }

    @Then("^receive from route ([^\"\\s]+) body$")
    public void receiveExchangeBodyMultiline(String endpointUri, String body) {
        receiveExchangeBody(endpointUri, body);
    }
    @Then("^receive from route ([^\"\\s]+) body: (.+)$")
    public void receiveExchangeBody(String endpointUri, String body) {
        runner.receive(action -> action.endpoint(camelEndpoint(endpointUri))
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
