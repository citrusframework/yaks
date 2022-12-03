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

import com.consol.citrus.endpoint.EndpointAdapter
import com.consol.citrus.endpoint.adapter.RequestDispatchingEndpointAdapter
import com.consol.citrus.endpoint.adapter.StaticEndpointAdapter
import com.consol.citrus.endpoint.adapter.mapping.HeaderMappingKeyExtractor
import com.consol.citrus.endpoint.adapter.mapping.SimpleMappingStrategy
import com.consol.citrus.http.message.HttpMessage
import com.consol.citrus.http.message.HttpMessageHeaders
import com.consol.citrus.message.Message
import com.consol.citrus.util.FileUtils
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

EndpointAdapter staticResponseAdapter() {
    RequestDispatchingEndpointAdapter dispatchingEndpointAdapter = new RequestDispatchingEndpointAdapter()

    Map<String, EndpointAdapter> mappings = new HashMap<>()

    mappings.put(HttpMethod.GET.name(), handleGet())
    mappings.put(HttpMethod.POST.name(), handlePost())
    mappings.put(HttpMethod.PUT.name(), handlePut())
    mappings.put(HttpMethod.DELETE.name(), handleDelete())

    SimpleMappingStrategy mappingStrategy = new SimpleMappingStrategy()
    mappingStrategy.setAdapterMappings(mappings)
    dispatchingEndpointAdapter.setMappingStrategy(mappingStrategy)

    dispatchingEndpointAdapter.setMappingKeyExtractor(new HeaderMappingKeyExtractor(HttpMessageHeaders.HTTP_REQUEST_METHOD))

    return dispatchingEndpointAdapter
}

static EndpointAdapter handleGet() {
    return new StaticEndpointAdapter() {
        @Override
        protected Message handleMessageInternal(Message message) {
            String requestUri = message.getHeader(HttpMessageHeaders.HTTP_REQUEST_URI)
            if (requestUri.endsWith("/openapi")) {
                String spec = FileUtils.readToString(FileUtils.getFileResource("fruitstore-v1.json"))
                return new HttpMessage(spec)
                        .contentType("application/json")
                        .status(HttpStatus.OK)
            } else if (requestUri.endsWith("/fruits/1000")) {
                return new HttpMessage("{\"id\": \"1000\", \"task\": \"Sample task\", \"completed\": 0}")
                        .contentType("application/json")
                        .status(HttpStatus.OK)
            } else if (requestUri.equals("/fruits")) {
                return new HttpMessage("[{\"id\": \"1000\", \"task\": \"Sample task\", \"completed\": 0}]")
                        .contentType("application/json")
                        .status(HttpStatus.OK)
            }

            return new HttpMessage()
                    .contentType("application/json")
                    .status(HttpStatus.NOT_FOUND)
        }
    }
}

static EndpointAdapter handlePost() {
    return new StaticEndpointAdapter() {
        @Override
        protected Message handleMessageInternal(Message message) {
            return new HttpMessage().status(HttpStatus.CREATED)
        }
    }
}

static EndpointAdapter handlePut() {
    return new StaticEndpointAdapter() {
        @Override
        protected Message handleMessageInternal(Message message) {
            return new HttpMessage().status(HttpStatus.OK)
        }
    }
}

static EndpointAdapter handleDelete() {
    return new StaticEndpointAdapter() {
        @Override
        protected Message handleMessageInternal(Message message) {
            return new HttpMessage().status(HttpStatus.NO_CONTENT)
        }
    }
}

http()
    .server()
    .port(8080)
    .endpointAdapter(staticResponseAdapter())
    .autoStart(true)
