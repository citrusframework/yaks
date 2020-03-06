package org.citrusframework.yaks.swagger;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "com.consol.citrus.cucumber.step.runner.core",
                 "org.citrusframework.yaks.swagger",
                 "org.citrusframework.yaks.http" },
        plugin = { "com.consol.citrus.cucumber.CitrusReporter" } )
public class SwaggerFeatureTest {
}
