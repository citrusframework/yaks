package com.company.steps.custom;

import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.annotations.CitrusResource;
import io.cucumber.java.en.Then;

import static com.consol.citrus.actions.EchoAction.Builder.echo;

public class CustomSteps {

	@CitrusResource
    private TestCaseRunner runner;

    @Then("^YAKS can be extended!$")
    public void yaksCanBeExtended() {
        runner.run(echo("YAKS can be extended!"));
    }

}
