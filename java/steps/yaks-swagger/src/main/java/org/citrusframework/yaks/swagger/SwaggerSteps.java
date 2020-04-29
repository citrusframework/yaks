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

package org.citrusframework.yaks.swagger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.consol.citrus.Citrus;
import com.consol.citrus.CitrusSettings;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusAnnotations;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.variable.dictionary.json.JsonPathMappingDataDictionary;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.swagger.models.ArrayModel;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import org.citrusframework.yaks.http.HttpClientSteps;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class SwaggerSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private HttpClientSteps clientSteps;

    private Swagger swagger;
    private Operation operation;

    private JsonPathMappingDataDictionary outboundDictionary;
    private JsonPathMappingDataDictionary inboundDictionary;

    @Before
    public void before(Scenario scenario) {
        clientSteps = new HttpClientSteps();
        CitrusAnnotations.injectAll(clientSteps, citrus);
        CitrusAnnotations.injectTestRunner(clientSteps, runner);
        clientSteps.before(scenario);

        operation = null;

        outboundDictionary = new JsonPathMappingDataDictionary();
        inboundDictionary = new JsonPathMappingDataDictionary();
    }

    @Given("^OpenAPI specification: ([^\\s]+)$")
    public void loadOpenAPISpec(String resource) {
        loadSwaggerResource(resource);
    }

    @Given("^Swagger resource: ([^\\s]+)$")
    public void loadSwaggerResource(String resource) {
        if (resource.startsWith("http")) {
            try {
                URL url = new URL(resource);
                if (resource.startsWith("https")) {
                    swagger = SwaggerResourceLoader.fromSecuredWebResource(url);
                } else {
                    swagger = SwaggerResourceLoader.fromWebResource(url);
                }
                clientSteps.setUrl(String.format("%s://%s%s/%s", url.getProtocol(), url.getHost(), url.getPort() > 0 ? ":" + url.getPort() : "", getBasePath()));
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to retrieve Swagger Open API specification as web resource: " + resource, e);
            }
        } else {
            swagger = SwaggerResourceLoader.fromFile(resource);

            Scheme scheme = Optional.ofNullable(swagger.getSchemes())
                    .orElse(Collections.singletonList(Scheme.HTTP))
                    .stream()
                    .filter(s -> s == Scheme.HTTP || s == Scheme.HTTPS)
                    .findFirst()
                    .orElse(Scheme.HTTP);

            clientSteps.setUrl(String.format("%s://%s/%s", scheme.toValue(), swagger.getHost(), getBasePath()));
        }
    }

    @Given("^outbound dictionary$")
    public void createOutboundDictionary(DataTable dataTable) {
        Map<String, String> mappings = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            outboundDictionary.getMappings().put(mapping.getKey(), mapping.getValue());
        }
    }

    @Given("^inbound dictionary$")
    public void createInboundDictionary(DataTable dataTable) {
        Map<String, String> mappings = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            inboundDictionary.getMappings().put(mapping.getKey(), mapping.getValue());
        }
    }

    @When("^(?:I|i)nvoke operation: (.+)$")
    public void invokeOperation(String operationId) {
        for (Map.Entry<String, Path> path : swagger.getPaths().entrySet()) {
            Optional<Map.Entry<HttpMethod, Operation>> operationEntry = path.getValue().getOperationMap().entrySet().stream()
                    .filter(op -> operationId.equals(op.getValue().getOperationId()))
                    .findFirst();

            if (operationEntry.isPresent()) {
                operation = operationEntry.get().getValue();
                sendRequest(path.getKey(), operationEntry.get().getKey(), operationEntry.get().getValue());
                break;
            }
        }
    }

    @Then("^(?:V|v)erify operation result: (\\d+)(?: [^\\s]+)?$")
    public void verifyResponseByStatus(int response) {
        receiveResponse(operation, String.valueOf(response));
    }

    @And("^(?:V|v)erify operation response: (.+)$")
    public void verifyResponseByName(String response) {
        receiveResponse(operation, response);
    }

    /**
     * Invoke request for given API operation. The request parameters, headers and payload are generated via specification
     * details in that operation.
     * @param path
     * @param method
     * @param operation
     */
    private void sendRequest(String path, HttpMethod method, Operation operation) {
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                    .filter(p -> p instanceof HeaderParameter)
                    .filter(Parameter::getRequired)
                    .forEach(p -> clientSteps.addRequestHeader(p.getName(), SwaggerTestDataGenerator.createRandomValueExpression(((HeaderParameter) p).getItems(), swagger.getDefinitions(), false)));

            operation.getParameters().stream()
                    .filter(param -> param instanceof QueryParameter)
                    .filter(Parameter::getRequired)
                    .forEach(param -> clientSteps.addRequestQueryParam(param.getName(), SwaggerTestDataGenerator.createRandomValueExpression((QueryParameter) param)));

            operation.getParameters().stream()
                    .filter(p -> p instanceof BodyParameter)
                    .filter(Parameter::getRequired)
                    .findFirst()
                    .ifPresent(p -> {
                        BodyParameter body = (BodyParameter) p;
                        if (body.getSchema() != null) {
                            clientSteps.setRequestBody(SwaggerTestDataGenerator.createOutboundPayload(body.getSchema(), swagger.getDefinitions()));

                            if ((body.getSchema() instanceof RefModel || body.getSchema() instanceof ArrayModel)) {
                                clientSteps.setOutboundDictionary(outboundDictionary);
                            }
                        }
                    });
        }

        String randomizedPath = path;
        if (operation.getParameters() != null) {
            List<PathParameter> pathParams = operation.getParameters().stream()
                    .filter(p -> p instanceof PathParameter)
                    .map(PathParameter.class::cast)
                    .collect(Collectors.toList());

            for (PathParameter parameter : pathParams) {
                String parameterValue;
                if (runner.getTestCase().getVariableDefinitions().containsKey(parameter.getName())) {
                    parameterValue = "\\" + CitrusSettings.VARIABLE_PREFIX + parameter.getName() + CitrusSettings.VARIABLE_SUFFIX;
                } else {
                    parameterValue = SwaggerTestDataGenerator.createRandomValueExpression(parameter);
                }
                randomizedPath = Pattern.compile("\\{" + parameter.getName() + "}")
                                        .matcher(randomizedPath)
                                        .replaceAll(parameterValue);
            }
        }

        if (operation.getConsumes() != null) {
            clientSteps.addRequestHeader(HttpHeaders.CONTENT_TYPE, operation.getConsumes().get(0));
        }

        clientSteps.sendClientRequest(method.name().toUpperCase(), randomizedPath);
    }

    /**
     * Verify operation response where expected parameters, headers and payload are generated using the operation specification details.
     * @param operation
     * @param status
     */
    private void receiveResponse(Operation operation, String status) {
        if (operation.getResponses() != null) {
            Response response = Optional.ofNullable(operation.getResponses().get(status))
                                        .orElse(operation.getResponses().get("default"));

            if (response != null) {
                if (response.getHeaders() != null) {
                    for (Map.Entry<String, Property> header : response.getHeaders().entrySet()) {
                        clientSteps.addResponseHeader(header.getKey(), SwaggerTestDataGenerator.createValidationExpression(header.getValue(), swagger.getDefinitions(), false));
                    }
                }

                if (response.getSchema() != null) {
                    clientSteps.setResponseBody(SwaggerTestDataGenerator.createInboundPayload(response.getSchema(), swagger.getDefinitions()));

                    if ((response.getSchema() instanceof RefProperty || response.getSchema() instanceof ArrayProperty)) {
                        clientSteps.setInboundDictionary(inboundDictionary);
                    }
                }
            }
        }

        if (operation.getProduces() != null) {
            clientSteps.addResponseHeader(HttpHeaders.CONTENT_TYPE, operation.getProduces().get(0));
        }

        if (Pattern.compile("[0-9]+").matcher(status).matches()) {
            clientSteps.receiveClientResponse(Integer.parseInt(status));
        } else {
            clientSteps.receiveClientResponse(HttpStatus.OK.value());
        }
    }

    /**
     * Gets the normalized base path of the service.
     * @return
     */
    private String getBasePath() {
        return Optional.ofNullable(swagger.getBasePath())
                       .map(basePath -> basePath.startsWith("/") ? basePath.substring(1) : basePath).orElse("");
    }

}
