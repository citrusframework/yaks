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

import java.util.Map;

import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusResource;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static com.consol.citrus.actions.EchoAction.Builder.echo;
import static com.consol.citrus.actions.SleepAction.Builder.sleep;

public class StandardSteps {

    @CitrusResource
    private TestCaseRunner runner;

    @Given("^YAKS does Cloud-Native BDD testing$")
    public void itDoesBDD() {
        print("YAKS does Cloud-Native BDD testing");
    }

    @Then("^YAKS rocks!$")
    public void yaksRocks() {
        print("YAKS rocks!");
    }

    @Given("^variable ([^\\s]+) is \"([^\"]*)\"$")
    public void variable(String name, String value) {
        runner.variable(name, value);
    }

    @Given("^variables$")
    public void variables(DataTable dataTable) {
        Map<String, String> variables = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            runner.variable(entry.getKey(), entry.getValue());
        }
    }

    @Then("^(?:log|print) '(.+)'$")
    public void print(String message) {
        runner.run(echo(message));
    }

    @Then("^(?:log|print)$")
    public void printMultiline(String message) {
        runner.run(echo(message));
    }

    @Then("^sleep$")
    public void doSleep() {
        runner.then(sleep());
    }

    @Then("^sleep (\\d+) ms$")
    public void doSleep(long milliseconds) {
        runner.then(sleep().milliseconds(milliseconds));
    }
}
