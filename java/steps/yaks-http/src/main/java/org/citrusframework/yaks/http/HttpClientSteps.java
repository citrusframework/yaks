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

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.CitrusSettings;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.container.Wait;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.actions.HttpClientActionBuilder;
import com.consol.citrus.http.actions.HttpClientRequestActionBuilder;
import com.consol.citrus.http.actions.HttpClientResponseActionBuilder;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.http.client.HttpClientBuilder;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.util.FileUtils;
import com.consol.citrus.variable.dictionary.DataDictionary;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;

import static com.consol.citrus.http.actions.HttpActionBuilder.http;
import static com.consol.citrus.validation.PathExpressionValidationContext.Builder.pathExpression;

/**
 * @author Christoph Deppisch
 */
public class HttpClientSteps implements HttpSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private HttpClient httpClient;

    private String requestUrl;

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

    private long timeout;

    private boolean forkMode = HttpSettings.getForkMode();

    @Before
    public void before(Scenario scenario) {
        if (httpClient == null) {
            if (citrus.getCitrusContext().getReferenceResolver().resolveAll(HttpClient.class).size() == 1L) {
                httpClient = citrus.getCitrusContext().getReferenceResolver().resolve(HttpClient.class);
            } else {
                httpClient = new HttpClientBuilder()
                        .timeout(HttpSettings.getTimeout())
                        .build();
            }
        }

        timeout = httpClient.getEndpointConfiguration().getTimeout();

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

    @Given("^HTTP client \"([^\"\\s]+)\"$")
    public void setClient(String id) {
        if (!citrus.getCitrusContext().getReferenceResolver().isResolvable(id)) {
            throw new CitrusRuntimeException("Unable to find http client for id: " + id);
        }

        httpClient = citrus.getCitrusContext().getReferenceResolver().resolve(id, HttpClient.class);
    }

    @Given("^(?:URL|url): ([^\\s]+)$")
    public void setUrl(String url) {
        if (url.startsWith("https")) {
            httpClient.getEndpointConfiguration().setRequestFactory(sslRequestFactory());
        }

        this.requestUrl = url;
    }

    @Given("^HTTP request timeout is (\\d+)(?: ms| milliseconds)$")
    public void configureTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Given("^HTTP request fork mode is (enabled|disabled)$")
    public void configureForkMode(String mode) {
        this.forkMode = "enabled".equals(mode);
    }

    @Given("^(?:URL|url) is healthy$")
    public void healthCheck() {
        waitForHttpUrl(requestUrl);
    }

    @Given("^(?:URL|url|path) ([^\\s]+) is healthy$")
    public void healthCheck(String urlOrPath) {
        waitForHttpUrl(getRequestUrl(urlOrPath));
    }

    @Given("^wait for (?:URL|url|path) ([^\\s]+)$")
    public void waitForHttpUrl(String urlOrPath) {
        waitForHttpStatus(getRequestUrl(urlOrPath), 200);
    }

    @Given("^wait for (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE) on (?:URL|url|path) ([^\\s]+)$")
    public void waitForHttpUrlUsingMethod(String method, String urlOrPath) {
        waitForHttpStatusUsingMethod(method, getRequestUrl(urlOrPath), 200);
    }

    @Given("^wait for (?:URL|url|path) ([^\\s]+) to return (\\d+)(?: [^\\s]+)?$")
    public void waitForHttpStatus(String urlOrPath, Integer statusCode) {
        runner.given(Wait.Builder.waitFor().http()
                .milliseconds(timeout)
                .interval(timeout / 10)
                .status(statusCode)
                .url(getRequestUrl(urlOrPath)));
    }

    @Given("^wait for (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE) on (?:URL|url|path) ([^\\s]+) to return (\\d+)(?: [^\\s]+)?$")
    public void waitForHttpStatusUsingMethod(String method, String urlOrPath, Integer statusCode) {
        runner.given(Wait.Builder.waitFor().http()
                .milliseconds(timeout)
                .method(method)
                .interval(timeout / 10)
                .status(statusCode)
                .url(getRequestUrl(urlOrPath)));
    }

    @Then("^(?:expect|verify) HTTP response header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addResponseHeader(String name, String value) {
        if (name.equals(HttpHeaders.CONTENT_TYPE)) {
            responseMessageType = getMessageType(value);
        }

        responseHeaders.put(name, value);
    }

    @Then("^(?:expect|verify) HTTP response headers$")
    public void addResponseHeaders(DataTable headers) {
        Map<String, String> headerPairs = headers.asMap(String.class, String.class);
        headerPairs.forEach(this::addResponseHeader);
    }

    @Given("^HTTP request header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addRequestHeader(String name, String value) {
        if (name.equals(HttpHeaders.CONTENT_TYPE)) {
            requestMessageType = getMessageType(value);
        }

        requestHeaders.put(name, value);
    }

    @Given("^HTTP request query parameter ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addRequestQueryParam(String name, String value) {
        requestParams.put(name, value);
    }

    @Given("^HTTP request headers$")
    public void addRequestHeaders(DataTable headers) {
        Map<String, String> headerPairs = headers.asMap(String.class, String.class);
        headerPairs.forEach(this::addRequestHeader);
    }

    @Then("^(?:expect|verify) HTTP response expression: ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addBodyValidationExpression(String name, String value) {
        bodyValidationExpressions.put(name, value);
    }

    @Then("^(?:expect|verify) HTTP response expressions$")
    public void addBodyValidationExpressions(DataTable validationExpressions) {
        Map<String, String> expressions = validationExpressions.asMap(String.class, String.class);
        expressions.forEach(this::addBodyValidationExpression);
    }

    @Given("^HTTP request body$")
    public void setRequestBodyMultiline(String body) {
        setRequestBody(body);
    }

    @Given("^load HTTP request body ([^\\s]+)$")
    public void loadRequestBody(String file) {
        try {
            setRequestBody(FileUtils.readToString(FileUtils.getFileResource(file)));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load body from file resource %s", file));
        }
    }

    @Given("^HTTP request body: (.+)$")
    public void setRequestBody(String body) {
        this.requestBody = body;
    }

    @Then("^(?:expect|verify) HTTP response body$")
    public void setResponseBodyMultiline(String body) {
        setResponseBody(body);
    }

    @Given("^(?:expect|verify) HTTP response body loaded from ([^\\s]+)$")
    public void loadResponseBody(String file) {
        try {
            setResponseBody(FileUtils.readToString(FileUtils.getFileResource(file)));
        } catch (IOException e) {
            throw new CitrusRuntimeException(String.format("Failed to load body from file resource %s", file));
        }
    }

    @Then("^(?:expect|verify) HTTP response body: (.+)$")
    public void setResponseBody(String body) {
        this.responseBody = body;
    }

    @When("^send HTTP request$")
    public void sendClientRequestFull(String requestData) {
        sendClientRequest(HttpMessage.fromRequestData(requestData));
    }

    @Then("^receive HTTP response$")
    public void receiveClientResponseFull(String responseData) {
        receiveClientResponse(HttpMessage.fromResponseData(responseData));
    }

    @When("^send (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE)$")
    public void sendClientRequestMultilineBody(String method) {
        sendClientRequest(method, null);
    }

    @When("^send (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE) ([^\"\\s]+)$")
    public void sendClientRequest(String method, String path) {
        sendClientRequest(createRequest(requestBody, requestHeaders, requestParams, method, path));
        requestBody = null;
        requestHeaders.clear();
        requestParams.clear();
    }

    @Then("^receive HTTP (\\d+)(?: [^\\s]+)?$")
    public void receiveClientResponse(Integer status) {
        receiveClientResponse(createResponse(responseBody, responseHeaders, status));
        responseBody = null;
        responseHeaders.clear();
    }

    /**
     * Sends client request.
     * @param request
     */
    private void sendClientRequest(HttpMessage request) {
        HttpClientActionBuilder.HttpClientSendActionBuilder sendBuilder = http().client(httpClient).send();
        HttpClientRequestActionBuilder requestBuilder;

        if (request.getRequestMethod() == null || request.getRequestMethod().equals(HttpMethod.POST)) {
            requestBuilder = sendBuilder.post().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.GET)) {
            requestBuilder = sendBuilder.get().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.PUT)) {
            requestBuilder = sendBuilder.put().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.DELETE)) {
            requestBuilder = sendBuilder.delete().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.HEAD)) {
            requestBuilder = sendBuilder.head().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.TRACE)) {
            requestBuilder = sendBuilder.trace().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.PATCH)) {
            requestBuilder = sendBuilder.patch().message(request);
        } else if (request.getRequestMethod().equals(HttpMethod.OPTIONS)) {
            requestBuilder = sendBuilder.options().message(request);
        } else {
            requestBuilder = sendBuilder.post().message(request);
        }

        requestBuilder.fork(forkMode);

        if (StringUtils.hasText(requestUrl)) {
            requestBuilder.uri(requestUrl);
        }

        requestBuilder.messageType(requestMessageType);

        if (outboundDictionary != null) {
            requestBuilder.dictionary(outboundDictionary);
        }

        runner.run(requestBuilder);
    }

    /**
     * Receives client response.
     * @param response
     */
    private void receiveClientResponse(HttpMessage response) {
        HttpClientResponseActionBuilder responseBuilder = http().client(httpClient).receive()
                .response(response.getStatusCode())
                .message(response);

        responseBuilder.validate(pathExpression().expressions(bodyValidationExpressions));
        bodyValidationExpressions.clear();

        responseBuilder.timeout(timeout);
        responseBuilder.messageType(responseMessageType);

        if (inboundDictionary != null) {
            responseBuilder.dictionary(inboundDictionary);
        }

        runner.run(responseBuilder);
    }

    /**
     * Get secure request factory.
     * @return
     */
    private HttpComponentsClientHttpRequestFactory sslRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(sslClient());
    }

    /**
     * Get secure http client implementation with trust all strategy and noop host name verifier.
     * @return
     */
    private org.apache.http.client.HttpClient sslClient() {
        try {
            SSLContext sslcontext = SSLContexts
                    .custom()
                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslcontext, NoopHostnameVerifier.INSTANCE);

            return HttpClients
                    .custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new CitrusRuntimeException("Failed to create http client for ssl connection", e);
        }
    }

    /**
     * Helper method concatenating base request URL and given relative URL resource path. In case given parameter us a full qualified
     * URL itself use this URL as a result. Adds error handling in case base request URL is not set properly and avoids duplicate path
     * separators in concatenated URLs.
     *
     * @param urlOrPath
     * @return
     */
    private String getRequestUrl(String urlOrPath) {
        if (StringUtils.hasText(urlOrPath) && urlOrPath.startsWith("http")) {
            return urlOrPath;
        }

        if (!StringUtils.hasText(requestUrl)) {
            throw new IllegalStateException("Must provide a base request URL first when using relative resource path: " + urlOrPath);
        }

        if (!StringUtils.hasText(urlOrPath) || urlOrPath.equals("/")) {
            return requestUrl;
        }

        return (requestUrl.endsWith("/") ? requestUrl : requestUrl + "/") + (urlOrPath.startsWith("/") ? urlOrPath.substring(1) : urlOrPath);
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
}
