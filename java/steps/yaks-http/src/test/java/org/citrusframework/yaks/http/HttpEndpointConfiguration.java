package org.citrusframework.yaks.http;

import java.util.HashMap;
import java.util.Map;

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
public class HttpEndpointConfiguration {

    private static final int HTTP_PORT = 8080;

    @Bean
    public HttpServer httpServer() {
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
                return new HttpMessage().status(HttpStatus.CREATED);
            }
        };
    }

    @Bean
    public EndpointAdapter handlePutRequestAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message request) {
                return new HttpMessage(request).status(HttpStatus.OK);
            }
        };
    }

    @Bean
    public EndpointAdapter handleGetRequestAdapter() {
        StaticResponseEndpointAdapter responseEndpointAdapter = new StaticResponseEndpointAdapter();
        responseEndpointAdapter.getMessageHeader().put(HttpMessageHeaders.HTTP_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        responseEndpointAdapter.getMessageHeader().put("X-TodoId", "citrus:randomNumber(5)");
        responseEndpointAdapter.setMessagePayload("{\"id\": \"citrus:randomNumber(5)\", \"task\": \"Sample task\", \"completed\": 0}");
        return responseEndpointAdapter;
    }

    @Bean
    public EndpointAdapter handleDeleteRequestAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message message) {
                return new HttpMessage().status(HttpStatus.NO_CONTENT);
            }
        };
    }
}
