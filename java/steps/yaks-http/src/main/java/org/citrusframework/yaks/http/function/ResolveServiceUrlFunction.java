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

package org.citrusframework.yaks.http.function;

import java.util.List;
import java.util.Locale;

import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.functions.Function;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.yaks.YaksSettings;

public class ResolveServiceUrlFunction implements Function {

    @Override
    public String execute(List<String> parameterList, TestContext context) {
        if (parameterList.isEmpty()) {
            throw new CitrusRuntimeException("Missing service name for resolve function");
        }

        String serviceName = parameterList.get(0);

        boolean secure = false;
        if (parameterList.size() > 1) {
            secure = Boolean.parseBoolean(parameterList.get(1).toLowerCase(Locale.US));
        }

        String scheme = "http://";
        if (secure) {
            scheme = "https://";
        }

        if (YaksSettings.isLocal()) {
            int port = 0;
            if (context.getReferenceResolver().isResolvable(serviceName)) {
                HttpServer server = context.getReferenceResolver().resolve(serviceName, HttpServer.class);
                port = server.getPort();
            }

            return String.format("%slocalhost%s", scheme, port > 0 ? ":" + port : "");
        } else {
            return String.format("%s%s.%s", scheme, serviceName, context.getVariable("YAKS_NAMESPACE"));
        }
    }
}
