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

package org.citrusframework.yaks.groovy.dsl.endpoints;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.consol.citrus.endpoint.EndpointBuilder;
import com.consol.citrus.endpoint.direct.DirectEndpoints;
import com.consol.citrus.http.endpoint.builder.HttpEndpoints;
import com.consol.citrus.jms.endpoint.JmsEndpoints;
import com.consol.citrus.kafka.endpoint.builder.KafkaEndpoints;
import com.consol.citrus.mail.endpoint.builder.MailEndpoints;
import com.consol.citrus.ws.endpoint.builder.WebServiceEndpoints;
import groovy.lang.GroovyRuntimeException;
import org.springframework.util.ReflectionUtils;

/**
 * Set of supported endpoints when adding configuration scripts via Groovy shell script.
 * @author Christoph Deppisch
 */
public enum Endpoints {
    DIRECT("direct", DirectEndpoints.class),
    JMS("jms", JmsEndpoints.class),
    HTTP("http", HttpEndpoints.class),
    SOAP("soap", WebServiceEndpoints.class),
    MAIL("mail", MailEndpoints.class),
    KAFKA("kafka", KafkaEndpoints.class);

    private final String id;
    private final Class<?> builderType;

    Endpoints(String id, Class<?> builderType) {
        this.id = id;
        this.builderType = builderType;
    }

    public String id() {
        return id;
    }

    public Class<?> builderType() {
        return builderType;
    }

    /**
     * Finds endpoint builder for given type. Usually the type is one of client/server or
     * asynchronous/synchronous.
     * @param type
     * @return
     */
    public EndpointBuilder<?> getEndpointBuilder(String type) {
        Method m = ReflectionUtils.findMethod(builderType, type);
        if (m == null) {
            throw new GroovyRuntimeException(String.format("Failed to find builder method %s for endpoint builder type %s", type, builderType.getName()));
        }

        try {
            Method initializer = ReflectionUtils.findMethod(builderType, id);
            if (initializer == null) {
                throw new GroovyRuntimeException(String.format("Failed to find initializing method %s for endpoint builder type %s", id, builderType.getName()));
            }
            return (EndpointBuilder<?>) m.invoke(initializer.invoke(null));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GroovyRuntimeException("Failed to get endpoint builder", e);
        }
    }
}
