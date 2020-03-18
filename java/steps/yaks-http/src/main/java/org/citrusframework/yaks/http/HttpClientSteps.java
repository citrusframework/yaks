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
import java.net.MalformedURLException;
import java.net.URL;
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
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.actions.HttpClientActionBuilder;
import com.consol.citrus.http.actions.HttpClientRequestActionBuilder;
import com.consol.citrus.http.actions.HttpClientResponseActionBuilder;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.http.client.HttpClientBuilder;
import com.consol.citrus.http.message.HttpMessage;
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

    private Map<String, String> bodyValidationExpressions = new HashMap<>();

    private String requestMessageType;
    private String responseMessageType;

    private String requestBody;
    private String responseBody;

    private DataDictionary outboundDictionary;
    private DataDictionary inboundDictionary;

    @Before
    public void before(Scenario scenario) {
        if (httpClient == null && citrus.getCitrusContext().getReferenceResolver().resolveAll(HttpClient.class).size() == 1L) {
            httpClient = citrus.getCitrusContext().getReferenceResolver().resolve(HttpClient.class);
        } else {
            httpClient = new HttpClientBuilder().build();
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

    @Given("^http-client \"([^\"\\s]+)\"$")
    public void setClient(String id) {
        if (!citrus.getCitrusContext().getReferenceResolver().isResolvable(id)) {
            throw new CitrusRuntimeException("Unable to find http client for id: " + id);
        }

        httpClient = citrus.getCitrusContext().getReferenceResolver().resolve(id, HttpClient.class);
    }

    @Given("^(?:URL|url): ([^\\s]+)$")
    public void setUrl(String url) {
        try {
            URL requestURL = new URL(url);
            if (requestURL.getProtocol().equalsIgnoreCase("https")) {
                httpClient.getEndpointConfiguration().setRequestFactory(sslRequestFactory());
            }

            this.requestUrl = url;
        } catch (MalformedURLException e) {
            throw new CitrusRuntimeException(e);
        }
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

    @Given("^HTTP request body: (.+)$")
    public void setRequestBody(String body) {
        this.requestBody = body;
    }

    @Then("^(?:expect|verify) HTTP response body$")
    public void setResponseBodyMultiline(String body) {
        setResponseBody(body);
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

        for (Map.Entry<String, String> headerEntry : bodyValidationExpressions.entrySet()) {
            responseBuilder.validate(headerEntry.getKey(), headerEntry.getValue());
        }
        bodyValidationExpressions.clear();

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
     * Specifies the inboundDictionary.
     *
     * @param inboundDictionary
     */
    public void setInboundDictionary(DataDictionary inboundDictionary) {
        this.inboundDictionary = inboundDictionary;
    }

    /**
     * Specifies the outboundDictionary.
     *
     * @param outboundDictionary
     */
    public void setOutboundDictionary(DataDictionary outboundDictionary) {
        this.outboundDictionary = outboundDictionary;
    }
}
