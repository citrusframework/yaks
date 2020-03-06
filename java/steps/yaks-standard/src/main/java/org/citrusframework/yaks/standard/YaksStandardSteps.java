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

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.runner.TestRunner;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class YaksStandardSteps {

    @CitrusResource
    private TestRunner runner;

    @Given("^YAKS does Cloud-Native BDD testing$")
    public void itDoesBDD() {
        print("YAKS does Cloud-Native BDD testing");
    }

    @Then("^YAKS rocks!$")
    public void yaksRocks() {
        print("YAKS rocks!");
    }

    @Then("^(?:log|print) '(.+)'$")
    public void print(String message) {
        runner.echo(message);
    }
}
