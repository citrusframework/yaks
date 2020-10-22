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

import com.consol.citrus.Citrus;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.server.Server;
import io.cucumber.java.en.Given;

import static com.consol.citrus.actions.PurgeEndpointAction.Builder.purgeEndpoints;
import static com.consol.citrus.actions.StartServerAction.Builder.start;
import static com.consol.citrus.actions.StopServerAction.Builder.stop;

/**
 * @author Christoph Deppisch
 */
public class EndpointSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @CitrusFramework
    private Citrus citrus;

    @Given("^start server component ([^\"\\s]+)$")
    public void startServer(String name) {
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            Server server = citrus.getCitrusContext().getReferenceResolver().resolve(name, Server.class);
            if (!server.isRunning()) {
                runner.run(start(server));
            }
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find server component '%s'", name));
        }
    }

    @Given("^stop server component ([^\"\\s]+)$")
    public void stopServer(String name) {
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            Server server = citrus.getCitrusContext().getReferenceResolver().resolve(name, Server.class);
            if (server.isRunning()) {
                runner.run(stop(server));
            }
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find server component '%s'", name));
        }
    }

    @Given("^purge endpoint ([^\"\\s]+)$")
    public void purgeEndpoint(String name) {
        if (citrus.getCitrusContext().getReferenceResolver().isResolvable(name)) {
            Endpoint endpoint = citrus.getCitrusContext().getReferenceResolver().resolve(name, Endpoint.class);
            runner.run(purgeEndpoints().endpoint(endpoint));
        } else {
            throw new CitrusRuntimeException(String.format("Unable to find endpoint '%s'", name));
        }
    }
}
