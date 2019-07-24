package dev.yaks.testing.http;

import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class HttpConfiguration {

    @Bean
    public HttpClient defaultHttpClient() {
        return CitrusEndpoints.http()
                              .client()
                              .build();
    }
}
