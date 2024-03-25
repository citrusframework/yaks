/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.citrusframework.endpoint.EndpointAdapter
import org.citrusframework.endpoint.adapter.RequestDispatchingEndpointAdapter
import org.citrusframework.endpoint.adapter.StaticEndpointAdapter
import org.citrusframework.endpoint.adapter.mapping.HeaderMappingKeyExtractor
import org.citrusframework.endpoint.adapter.mapping.SimpleMappingStrategy
import org.citrusframework.http.message.HttpMessage
import org.citrusframework.http.message.HttpMessageHeaders
import org.citrusframework.message.Message
import org.citrusframework.spi.Resources
import org.citrusframework.util.FileUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

EndpointAdapter templateResponseAdapter() {
    RequestDispatchingEndpointAdapter dispatchingEndpointAdapter = new RequestDispatchingEndpointAdapter()

    Map<String, EndpointAdapter> mappings = new HashMap<>()
    mappings.put("/", indexPageHandler())
    mappings.put("/favicon.ico", faviconHandler())

    SimpleMappingStrategy mappingStrategy = new SimpleMappingStrategy()
    mappingStrategy.setAdapterMappings(mappings)
    dispatchingEndpointAdapter.setMappingStrategy(mappingStrategy)

    dispatchingEndpointAdapter.setMappingKeyExtractor(new HeaderMappingKeyExtractor(HttpMessageHeaders.HTTP_REQUEST_URI))

    return dispatchingEndpointAdapter
}

static EndpointAdapter indexPageHandler() {
    return new StaticEndpointAdapter() {
        @Override
        protected Message handleMessageInternal(Message message) {
            try {
                return new HttpMessage(FileUtils.readToString(Resources.fromClasspath("index.html")))
                        .contentType(MediaType.TEXT_HTML_VALUE)
                        .status(HttpStatus.OK)
            } catch (IOException ignored) {
                return new HttpMessage().status(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }
}

static EndpointAdapter faviconHandler() {
    return new StaticEndpointAdapter() {
        @Override
        protected Message handleMessageInternal(Message message) {
            return new HttpMessage().status(HttpStatus.OK)
        }
    }
}

http()
    .server()
    .port(8080)
    .endpointAdapter(templateResponseAdapter())
    .autoStart(true)
