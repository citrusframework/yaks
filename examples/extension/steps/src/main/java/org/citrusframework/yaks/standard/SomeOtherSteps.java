// TODO package can be changed when we add support for loading
// steps from any location
package org.citrusframework.yaks.standard;

import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.runner.TestRunner;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class SomeOtherSteps {

	@CitrusResource
    private TestRunner runner;

    @Then("^YAKS can be extended!$")
    public void yaksCanBeExtended() {
        runner.echo("YAKS can be extended!");
    }

}
