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

package org.citrusframework.yaks.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.CitrusSettings;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.actions.HttpServerActionBuilder;
import com.consol.citrus.http.actions.HttpServerRequestActionBuilder;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.util.FileUtils;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static com.consol.citrus.validation.PathExpressionValidationContext.Builder.pathExpression;

/**
 * @author Christoph Deppisch
 */
public class HttpServerSteps implements HttpSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private HttpServer httpServer;

    private Map<String, String> requestHeaders = new HashMap<>();
    private Map<String, String> responseHeaders = new HashMap<>();
    private Map<String, String> requestParams = new HashMap<>();

    private Map<String, Object> bodyValidationExpressions = new HashMap<>();

    private String requestMessageType;
    private String responseMessageType;

    private String requestBody;
    private String responseBody;

    private int serverPort = HttpSettings.getServerPort();
    private String serverName = HttpSettings.getServerName();

    private long timeout = HttpSettings.getTimeout();

    @Before
    public void before(Scenario scenario) {
        if (httpServer == null) {
            if (citrus.getCitrusContext().getReferenceResolver().resolveAll(HttpServer.class).size() == 1L) {
                httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(HttpServer.class);
                serverPort = httpServer.getPort();
                timeout = httpServer.getDefaultTimeout();
            } else if (citrus.getCitrusContext().getReferenceResolver().isResolvable(serverName)) {
                httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(serverName, HttpServer.class);
                serverPort = httpServer.getPort();
                timeout = httpServer.getDefaultTimeout();
            } else {
                httpServer = new HttpServerBuilder()
                        .timeout(timeout)
                        .port(serverPort)
                        .name(serverName)
                        .build();

                citrus.getCitrusContext().getReferenceResolver().bind(serverName, httpServer);
                httpServer.initialize();
            }
        }

        requestHeaders = new HashMap<>();
        responseHeaders = new HashMap<>();
        requestParams = new HashMap<>();
        requestMessageType = CitrusSettings.DEFAULT_MESSAGE_TYPE;
        responseMessageType = CitrusSettings.DEFAULT_MESSAGE_TYPE;
        requestBody = null;
        responseBody = null;
        bodyValidationExpressions = new HashMap<>();
    }

    @Given("^HTTP server \"([^\"\\s]+)\"$")
    public void setServer(String name) {
        this.serverName = name;
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(name, HttpServer.class);
        } else if (httpServer != null) {
            citrus.getCitrusContext().getReferenceResolver().bind(serverName, httpServer);
            httpServer.setName(serverName);
        }
    }

    @Given("^HTTP server listening on port (\\d+)$")
    public void createServer(int port) {
        this.serverPort = port;
        if (httpServer != null) {
            httpServer.setPort(port);

            if (!httpServer.isRunning()) {
                httpServer.start();
            }
        }
    }

    @Given("^HTTP server timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Then("^(?:expect|verify) HTTP request header: ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addRequestHeader(String name, String value) {
        if (name.equals(HttpHeaders.CONTENT_TYPE)) {
            requestMessageType = getMessageType(value);
        }

        requestHeaders.put(name, value);
    }

    @Then("^(?:expect|verify) HTTP request headers$")
    public void addRequestHeaders(DataTable headers) {
        Map<String, String> headerPairs = headers.asMap(String.class, String.class);
        headerPairs.forEach(this::addRequestHeader);
    }

    @Given("^(?:expect|verify) HTTP request query parameter ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addRequestQueryParam(String name, String value) {
        requestParams.put(name, value);
    }

    @Given("^HTTP response header: ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addResponseHeader(String name, String value) {
        if (name.equals(HttpHeaders.CONTENT_TYPE)) {
            responseMessageType = getMessageType(value);
        }

        responseHeaders.put(name, value);
    }

    @Given("^HTTP response headers$")
    public void addResponseHeaders(DataTable headers) {
        Map<String, String> headerPairs = headers.asMap(String.class, String.class);
        headerPairs.forEach(this::addResponseHeader);
    }

    @Then("^(?:expect|verify) HTTP request expression: ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addBodyValidationExpression(String name, String value) {
        bodyValidationExpressions.put(name, value);
    }

    @Then("^(?:expect|verify) HTTP request expressions$")
    public void addBodyValidationExpressions(DataTable validationExpressions) {
        Map<String, String> expressions = validationExpressions.asMap(String.class, String.class);
        expressions.forEach(this::addBodyValidationExpression);
    }

    @Given("^HTTP response body$")
    public void setResponseBodyMultiline(String body) {
        setResponseBody(body);
    }

    @Given("^load HTTP response body ([^\\s]+)$")
    public void loadResponseBody(String file) {
        try {
            setResponseBody(FileUtils.readToString(FileUtils.getFileResource(file)));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load body from file resource %s", file));
        }
    }

    @Given("^HTTP response body: (.+)$")
    public void setResponseBody(String body) {
        this.responseBody = body;
    }

    @Then("^(?:expect|verify) HTTP request body$")
    public void setRequestBodyMultiline(String body) {
        setRequestBody(body);
    }

    @Then("^(?:expect|verify) HTTP request body loaded from ([^\\s]+)$")
    public void loadRequestBody(String file) {
        try {
            setRequestBody(FileUtils.readToString(FileUtils.getFileResource(file)));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load body from file resource %s", file));
        }
    }

    @Then("^(?:expect|verify) HTTP request body: (.+)$")
    public void setRequestBody(String body) {
        this.requestBody = body;
    }

    @When("^receive HTTP request$")
    public void receiveServerRequestFull(String requestData) {
        receiveServerRequest(HttpMessage.fromRequestData(requestData));
    }

    @Then("^send HTTP response$")
    public void sendServerResponseFull(String responseData) {
        sendServerResponse(HttpMessage.fromResponseData(responseData));
    }

    @When("^receive (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE)$")
    public void receiveServerRequestMultilineBody(String method) {
        receiveServerRequest(method, null);
    }

    @When("^receive (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE) ([^\"\\s]+)$")
    public void receiveServerRequest(String method, String path) {
        receiveServerRequest(createRequest(requestBody, requestHeaders, requestParams, method, path));
        requestBody = null;
        requestHeaders.clear();
        requestParams.clear();
    }

    @Then("^send HTTP (\\d+)(?: [^\\s]+)?$")
    public void sendServerResponse(Integer status) {
        sendServerResponse(createResponse(responseBody, responseHeaders, status));
        responseBody = null;
        responseHeaders.clear();
    }

    /**
     * Receives server request.
     * @param request
     */
    private void receiveServerRequest(HttpMessage request) {
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        HttpServerActionBuilder.HttpServerReceiveActionBuilder receiveBuilder = http().server(httpServer).receive();
        HttpServerRequestActionBuilder requestBuilder;

        if (request.getRequestMethod() == null || request.getRequestMethod().equals(HttpMethod.POST)) {
            requestBuilder = receiveBuilder.post().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.GET)) {
            requestBuilder = receiveBuilder.get().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.PUT)) {
            requestBuilder = receiveBuilder.put().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.DELETE)) {
            requestBuilder = receiveBuilder.delete().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.HEAD)) {
            requestBuilder = receiveBuilder.head().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.TRACE)) {
            requestBuilder = receiveBuilder.trace().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.PATCH)) {
            requestBuilder = receiveBuilder.patch().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.OPTIONS)) {
            requestBuilder = receiveBuilder.options().message(request);
        } else {
            requestBuilder = receiveBuilder.post().message(request);
        }

        requestBuilder.validate(pathExpression().expressions(bodyValidationExpressions));
        bodyValidationExpressions.clear();

        requestBuilder
                .timeout(timeout)
                .messageType(requestMessageType);

        runner.run(requestBuilder);
    }

    /**
     * Sends server response.
     * @param response
     */
    private void sendServerResponse(HttpMessage response) {
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        runner.run(http().server(httpServer).send()
                .response(response.getStatusCode())
                .messageType(responseMessageType)
                .message(response));
    }

}
