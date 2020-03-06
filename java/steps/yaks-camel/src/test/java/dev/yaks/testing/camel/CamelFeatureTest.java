package dev.yaks.testing.camel;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "dev.yaks.testing.camel" },
        plugin = { "com.consol.citrus.cucumber.CitrusReporter" } )
public class CamelFeatureTest {
}
