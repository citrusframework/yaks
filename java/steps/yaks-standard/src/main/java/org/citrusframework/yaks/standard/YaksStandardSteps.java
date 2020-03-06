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
