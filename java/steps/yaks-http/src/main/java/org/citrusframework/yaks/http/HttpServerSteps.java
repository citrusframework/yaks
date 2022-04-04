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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.consol.citrus.Citrus;
import com.consol.citrus.CitrusSettings;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.actions.HttpServerActionBuilder;
import com.consol.citrus.http.actions.HttpServerRequestActionBuilder;
import com.consol.citrus.http.actions.HttpServerResponseActionBuilder;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.util.FileUtils;
import com.consol.citrus.variable.dictionary.DataDictionary;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

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

    private DataDictionary<?> outboundDictionary;
    private DataDictionary<?> inboundDictionary;

    private int securePort = HttpSettings.getSecurePort();
    private int serverPort = HttpSettings.getServerPort();
    private String serverName = HttpSettings.getServerName();

    private String sslKeyStorePath = HttpSettings.getSslKeyStorePath();
    private String sslKeyStorePassword = HttpSettings.getSslKeyStorePassword();

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
        outboundDictionary = null;
        inboundDictionary = null;
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

    @Given("^HTTP server \"([^\"\\s]+)\" with configuration$")
    public void setServerWithProperties(String name, DataTable properties) {
        setServer(name);
        configureServer(properties.asMap(String.class, String.class));
    }

    @Given("^(?:create|new) HTTP server \"([^\"\\s]+)\"$")
    public void newServer(String name) {
        this.serverName = name;
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(name, HttpServer.class);
        } else {
            httpServer = getOrCreateHttpServer();
        }
    }

    @Given("^(?:create|new) HTTP server \"([^\"\\s]+)\" with configuration$")
    public void newServerWithProperties(String name, DataTable properties) {
        newServer(name);
        configureServer(properties.asMap(String.class, String.class));
    }

    @Given("^HTTP server listening on port (\\d+)$")
    public void setServerPort(int port) {
        this.serverPort = port;
        getOrCreateHttpServer().setPort(port);
    }

    @Given("^HTTP server secure port (\\d+)$")
    public void setSecureServerPort(int port) {
        this.securePort = port;
        getOrCreateHttpServer().setConnector(sslConnector());
    }

    @Given("^enable secure HTTP server$")
    public void enableSecureConnector() {
        getOrCreateHttpServer().setConnector(sslConnector());
    }

    @Given("^HTTP server SSL keystore path ([^\\s]+)$")
    public void setSslKeyStorePath(String sslKeyStorePath) {
        this.sslKeyStorePath = sslKeyStorePath;
    }

    @Given("^HTTP server SSL keystore password ([^\\s]+)$")
    public void setSslKeyStorePassword(String sslKeyStorePassword) {
        this.sslKeyStorePassword = sslKeyStorePassword;
    }

    @Given("^start HTTP server$")
    public void startServer() {
        HttpServer httpServer = getOrCreateHttpServer();
        if (!httpServer.isRunning()) {
            httpServer.start();
        }
    }

    @Given("^stop HTTP server$")
    public void stopServer() {
        HttpServer httpServer = getOrCreateHttpServer();
        if (httpServer.isRunning()) {
            httpServer.stop();
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
    public void receiveServerRequest(HttpMessage request) {
        HttpServer httpServer = getOrCreateHttpServer();
        if (!httpServer.isRunning()) {
            httpServer.start();
        }

        HttpServerActionBuilder.HttpServerReceiveActionBuilder receiveBuilder = http().server(httpServer).receive();
        HttpServerRequestActionBuilder.HttpMessageBuilderSupport requestBuilder;

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
                .type(requestMessageType);

        if (inboundDictionary != null) {
            requestBuilder.dictionary(inboundDictionary);
        }

        runner.run(requestBuilder);
    }

    /**
     * Create a new server instance and bind it to the context.
     * @return
     */
    public HttpServer getOrCreateHttpServer() {
        if (httpServer != null) {
            if (httpServer.getName().equals(serverName)) {
                return httpServer;
            } else if (citrus.getCitrusContext().getReferenceResolver().isResolvable(serverName)) {
                httpServer = citrus.getCitrusContext().getReferenceResolver().resolve(serverName, HttpServer.class);
                serverPort = httpServer.getPort();
                timeout = httpServer.getDefaultTimeout();
                return httpServer;
            }
        }

        httpServer = new HttpServerBuilder()
                .timeout(timeout)
                .port(serverPort)
                .name(serverName)
                .build();

        citrus.getCitrusContext().getReferenceResolver().bind(serverName, httpServer);
        httpServer.initialize();

        return httpServer;
    }

    /**
     * Configure server from given properties map.
     * @param settings
     */
    private void configureServer(Map<String, String> settings) {
        setServerPort(Optional.ofNullable(settings.get("port")).map(Integer::parseInt).orElse(serverPort));
        configureTimeout(Optional.ofNullable(settings.get("timeout")).map(Long::valueOf).orElse(timeout));

        setSslKeyStorePath(settings.getOrDefault("sslKeyStorePath", sslKeyStorePath));
        setSslKeyStorePassword(settings.getOrDefault("sslKeyStorePassword", sslKeyStorePassword));

        if (Boolean.parseBoolean(settings.getOrDefault("secure", "false"))) {
            enableSecureConnector();
        }

        if (settings.containsKey("securePort")) {
            setSecureServerPort(Integer.parseInt(settings.get("securePort")));
        }
    }

    /**
     * Sends server response.
     * @param response
     */
    public void sendServerResponse(HttpMessage response) {
        response.setType(responseMessageType);

        HttpServerResponseActionBuilder.HttpMessageBuilderSupport responseBuilder = http().server(httpServer)
                .send()
                .response(response.getStatusCode())
                .message(response);

        if (outboundDictionary != null) {
            responseBuilder.dictionary(outboundDictionary);
        }


        runner.run(responseBuilder);
    }

    /**
     * Sends server response.
     * @param status
     */
    public void sendServerResponse(HttpStatus status) {
        runner.run(http().server(httpServer)
                .send()
                .response(status));
    }

    private ServerConnector sslConnector() {
        ServerConnector connector = new ServerConnector(new Server(),
                new SslConnectionFactory(sslContextFactory(), "http/1.1"),
                new HttpConnectionFactory(httpConfiguration()));
        connector.setPort(securePort);
        return connector;
    }

    private HttpConfiguration httpConfiguration() {
        HttpConfiguration parent = new HttpConfiguration();
        parent.setSecureScheme("https");
        parent.setSecurePort(securePort);
        HttpConfiguration configuration = new HttpConfiguration(parent);
        configuration.setCustomizers(Collections.singletonList(new SecureRequestCustomizer()));
        return configuration;
    }

    private SslContextFactory sslContextFactory() {
        try {
            SslContextFactory.Server contextFactory = new SslContextFactory.Server();
            contextFactory.setKeyStorePath(getKeyStorePathPath());
            contextFactory.setKeyStorePassword(sslKeyStorePassword);
            return contextFactory;
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to read keystore file in path: " + sslKeyStorePath);
        }
    }

    private String getKeyStorePathPath() throws IOException {
        if (sslKeyStorePath.equals(HttpSettings.SECURE_KEYSTORE_PATH_DEFAULT)) {
            File tmpKeyStore = File.createTempFile("http-server", ".jks");

            try (InputStream in = FileUtils.getFileResource(sslKeyStorePath).getInputStream()) {
                Files.copy(in, tmpKeyStore.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return tmpKeyStore.getPath();
            }
        } else {
            return FileUtils.getFileResource(sslKeyStorePath).getURL().getFile();
        }
    }

    /**
     * Specifies the inboundDictionary.
     *
     * @param inboundDictionary
     */
    public void setInboundDictionary(DataDictionary<?> inboundDictionary) {
        this.inboundDictionary = inboundDictionary;
    }

    /**
     * Specifies the outboundDictionary.
     *
     * @param outboundDictionary
     */
    public void setOutboundDictionary(DataDictionary<?> outboundDictionary) {
        this.outboundDictionary = outboundDictionary;
    }

    /**
     * Specify the request message type.
     * @param requestType
     */
    public void setRequestMessageType(String requestType) {
        this.requestMessageType = requestType;
    }
}
