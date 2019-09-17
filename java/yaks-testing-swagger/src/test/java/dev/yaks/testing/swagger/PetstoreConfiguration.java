package dev.yaks.testing.swagger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.endpoint.EndpointAdapter;
import com.consol.citrus.endpoint.adapter.RequestDispatchingEndpointAdapter;
import com.consol.citrus.endpoint.adapter.StaticEndpointAdapter;
import com.consol.citrus.endpoint.adapter.StaticResponseEndpointAdapter;
import com.consol.citrus.endpoint.adapter.mapping.HeaderMappingKeyExtractor;
import com.consol.citrus.endpoint.adapter.mapping.SimpleMappingStrategy;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.message.HttpMessageHeaders;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.message.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class PetstoreConfiguration {

    private static final int HTTP_PORT = 8080;

    @Bean
    public HttpServer petstoreServer() {
        return CitrusEndpoints.http()
                              .server()
                              .port(HTTP_PORT)
                              .autoStart(true)
                              .endpointAdapter(staticResponseAdapter())
                              .build();
    }

    @Bean
    public EndpointAdapter staticResponseAdapter() {
        RequestDispatchingEndpointAdapter dispatchingEndpointAdapter = new RequestDispatchingEndpointAdapter();

        Map<String, EndpointAdapter> mappings = new HashMap<>();

        mappings.put(HttpMethod.GET.name(), handleGetRequestAdapter());
        mappings.put(HttpMethod.POST.name(), handlePostRequestAdapter());
        mappings.put(HttpMethod.PUT.name(), handlePutRequestAdapter());
        mappings.put(HttpMethod.DELETE.name(), handleDeleteRequestAdapter());

        SimpleMappingStrategy mappingStrategy = new SimpleMappingStrategy();
        mappingStrategy.setAdapterMappings(mappings);
        dispatchingEndpointAdapter.setMappingStrategy(mappingStrategy);

        dispatchingEndpointAdapter.setMappingKeyExtractor(new HeaderMappingKeyExtractor(HttpMessageHeaders.HTTP_REQUEST_METHOD));

        return dispatchingEndpointAdapter;
    }

    @Bean
    public EndpointAdapter handlePostRequestAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message message) {
                return new HttpMessage()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .status(HttpStatus.CREATED);
            }
        };
    }

    @Bean
    public EndpointAdapter handlePutRequestAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message request) {
                return new HttpMessage()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .status(HttpStatus.OK);
            }
        };
    }

    @Bean
    public EndpointAdapter handleGetRequestAdapter() {
        return new StaticResponseEndpointAdapter() {
            private TestContext context;

            @Override
            public Message handleMessageInternal(Message request) {
                context = super.getTestContext();
                getMessageHeader().clear();
                setMessagePayload("");

                String requestUri = Optional.ofNullable(request.getHeader(HttpMessageHeaders.HTTP_REQUEST_URI))
                                            .map(Object::toString)
                                            .orElse("/openapi.json");

                if (requestUri.endsWith("openapi.json")) {
                     setMessagePayload("citrus:readFile('classpath:dev/yaks/testing/swagger/petstore-api.json')");
                } else {
                    int petId = Integer.parseInt(requestUri.substring(requestUri.lastIndexOf("/") + 1));
                    getMessageHeader().put(HttpMessageHeaders.HTTP_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

                    if (petId > 0) {
                        getTestContext().setVariable("petId", petId);
                        setMessagePayload("citrus:readFile('classpath:dev/yaks/testing/swagger/pet.json')");
                    } else {
                        getMessageHeader().put(HttpMessageHeaders.HTTP_STATUS_CODE, HttpStatus.NOT_FOUND);
                    }
                }

                return super.handleMessageInternal(request);
            }

            @Override
            protected TestContext getTestContext() {
                if (context == null) {
                    context = super.getTestContext();
                }
                return context;
            }
        };
    }

    @Bean
    public EndpointAdapter handleDeleteRequestAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message message) {
                return new HttpMessage()
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .status(HttpStatus.NO_CONTENT);
            }
        };
    }
}
