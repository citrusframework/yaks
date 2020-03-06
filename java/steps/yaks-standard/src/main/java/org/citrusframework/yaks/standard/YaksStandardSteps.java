package org.citrusframework.yaks.standard;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.runner.TestRunner;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class YaksStandardSteps {

    @CitrusResource
    private TestRunner runner;

    @Given("^Yaks does BDD testing on Kubernetes$")
    public void itDoesBDD() {
        print("Yaks does BDD testing on Kubernetes");
    }

    @Then("^Yaks is cool!$")
    public void yaksRocks() {
        print("Yaks is cool!");
    }

    @Then("^(?:log|print) '(.+)'$")
    public void print(String message) {
        runner.echo(message);
    }
}
