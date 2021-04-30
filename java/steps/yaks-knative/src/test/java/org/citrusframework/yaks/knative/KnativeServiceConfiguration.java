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

package org.citrusframework.yaks.knative;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import com.consol.citrus.endpoint.EndpointAdapter;
import com.consol.citrus.endpoint.adapter.StaticEndpointAdapter;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.message.Message;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.mock.KnativeMockServer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesCrudDispatcher;
import io.fabric8.mockwebserver.Context;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class KnativeServiceConfiguration {

    private static final int HTTP_PORT = 8080;

    private final KnativeMockServer knativeServer = new KnativeMockServer(new Context(), new MockWebServer(),
            new HashMap<>(), new KubernetesCrudDispatcher(), false);

    @Bean(destroyMethod = "destroy")
    public KnativeMockServer knativeMockServer() throws UnknownHostException {
        knativeServer.start(InetAddress.getLocalHost(), 0);
        return knativeServer;
    }

    @Bean(destroyMethod = "close")
    @DependsOn("knativeMockServer")
    public KnativeClient knativeClient() {
        return knativeServer.createKnative();
    }

    @Bean(destroyMethod = "close")
    @DependsOn("knativeMockServer")
    public KubernetesClient kubernetesClient() {
        return knativeServer.createClient();
    }

    @Bean
    public HttpServer httpServer() {
        return new HttpServerBuilder()
                              .port(HTTP_PORT)
                              .autoStart(true)
                              .endpointAdapter(handleCloudEventAdapter())
                              .build();
    }

    @Bean
    public EndpointAdapter handleCloudEventAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message message) {
                Assertions.assertThat(message.getHeader("Ce-Id")).isEqualTo("say-hello");
                Assertions.assertThat(message.getHeader("Ce-Specversion")).isEqualTo("1.0");
                Assertions.assertThat(message.getHeader("Ce-Subject")).isEqualTo("hello");
                Assertions.assertThat(message.getHeader("Ce-Type")).isEqualTo("greeting");
                Assertions.assertThat(message.getHeader("Ce-Source")).isEqualTo("https://github.com/citrusframework/yaks");
                Assertions.assertThat(message.getHeader("Content-Type").toString()).isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);
                Assertions.assertThat(message.getPayload(String.class)).isEqualTo("{\"msg\": \"Hello Knative!\"}");

                return new HttpMessage().status(HttpStatus.ACCEPTED);
            }
        };
    }
}
