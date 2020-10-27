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

package org.citrusframework.yaks.standard;

import com.consol.citrus.camel.endpoint.CamelSyncEndpoint;
import com.consol.citrus.camel.endpoint.CamelSyncEndpointConfiguration;
import com.consol.citrus.endpoint.direct.DirectEndpoint;
import com.consol.citrus.endpoint.direct.DirectEndpoints;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.citrusframework.yaks.report.SystemOutTestReporter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class EndpointConfiguration {

    private CamelContext camelContext = new SpringCamelContext();

    @Bean
    public DirectEndpoint fooEndpoint() {
        return DirectEndpoints
                          .direct()
                          .asynchronous()
                          .queue("foo")
                          .build();
    }

    @Bean
    @DependsOn("camelContext")
    public CamelSyncEndpoint echoEndpoint() {
        CamelSyncEndpointConfiguration endpointConfiguration = new CamelSyncEndpointConfiguration();
        endpointConfiguration.setCamelContext(camelContext);
        endpointConfiguration.setEndpointUri("direct:echo");
        return new CamelSyncEndpoint(endpointConfiguration);
    }

    @Bean
    public CamelContext camelContext() {
        RouteBuilder routeBuilder = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:echo")
                    .transform()
                    .simple("You just said: ${body}");
            }
        };

        try {
            camelContext.addRoutes(routeBuilder);
        } catch (Exception e) {
            throw new BeanCreationException("Failed to create Camel context", e);
        }
        return camelContext;
    }

    @Bean(destroyMethod = "destroy")
    public SystemOutTestReporter systemOutReporter() {
        return new SystemOutTestReporter();
    }
}
