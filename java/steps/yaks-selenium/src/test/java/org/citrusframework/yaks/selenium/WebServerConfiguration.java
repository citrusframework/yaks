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

package org.citrusframework.yaks.selenium;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.container.AfterSuite;
import com.consol.citrus.container.SequenceAfterSuite;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.endpoint.EndpointAdapter;
import com.consol.citrus.endpoint.adapter.RequestDispatchingEndpointAdapter;
import com.consol.citrus.endpoint.adapter.StaticEndpointAdapter;
import com.consol.citrus.endpoint.adapter.mapping.HeaderMappingKeyExtractor;
import com.consol.citrus.endpoint.adapter.mapping.SimpleMappingStrategy;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.http.message.HttpMessageHeaders;
import com.consol.citrus.http.server.HttpServer;
import com.consol.citrus.http.server.HttpServerBuilder;
import com.consol.citrus.message.Message;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import com.consol.citrus.selenium.endpoint.SeleniumBrowserBuilder;
import com.consol.citrus.util.FileUtils;
import org.citrusframework.yaks.selenium.page.UserFormPage;
import org.openqa.selenium.remote.Browser;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.consol.citrus.selenium.actions.SeleniumActionBuilder.selenium;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class WebServerConfiguration {

    private static final int HTTP_PORT = 8080;

    @Bean
    public SeleniumBrowser seleniumBrowser() {
        return new SeleniumBrowserBuilder()
                .type(Browser.HTMLUNIT.browserName())
                .build();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public UserFormPage userForm() {
        return new UserFormPage();
    }

    @Bean
    public HttpServer webServer() {
        return new HttpServerBuilder()
                .port(HTTP_PORT)
                .autoStart(true)
                .endpointAdapter(templateResponseAdapter())
                .build();
    }

    @Bean
    @DependsOn("seleniumBrowser")
    public AfterSuite afterSuite(SeleniumBrowser browser) {
        return new SequenceAfterSuite() {
            @Override
            public void doExecute(TestContext context) {
                selenium().browser(browser).stop()
                        .build()
                        .execute(context);
            }
        };
    }

    @Bean
    public EndpointAdapter templateResponseAdapter() {
        RequestDispatchingEndpointAdapter dispatchingEndpointAdapter = new RequestDispatchingEndpointAdapter();

        Map<String, EndpointAdapter> mappings = new HashMap<>();

        mappings.put("/", indexPageHandler());
        mappings.put("/form", userFormPageHandler());
        mappings.put("/favicon.ico", faviconHandler());

        SimpleMappingStrategy mappingStrategy = new SimpleMappingStrategy();
        mappingStrategy.setAdapterMappings(mappings);
        dispatchingEndpointAdapter.setMappingStrategy(mappingStrategy);

        dispatchingEndpointAdapter.setMappingKeyExtractor(new HeaderMappingKeyExtractor(HttpMessageHeaders.HTTP_REQUEST_URI));

        return dispatchingEndpointAdapter;
    }

    @Bean
    public EndpointAdapter indexPageHandler() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message request) {
                try {
                    return new HttpMessage(FileUtils.readToString(new ClassPathResource("templates/index.html")))
                            .contentType(MediaType.TEXT_HTML_VALUE)
                            .status(HttpStatus.OK);
                } catch (IOException e) {
                    return new HttpMessage().status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @Bean
    public EndpointAdapter userFormPageHandler() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message request) {
                try {
                    return new HttpMessage(FileUtils.readToString(new ClassPathResource("templates/form.html")))
                            .contentType(MediaType.TEXT_HTML_VALUE)
                            .status(HttpStatus.OK);
                } catch (IOException e) {
                    return new HttpMessage().status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @Bean
    public EndpointAdapter faviconHandler() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message request) {
                return new HttpMessage()
                        .status(HttpStatus.OK);
            }
        };
    }
}
