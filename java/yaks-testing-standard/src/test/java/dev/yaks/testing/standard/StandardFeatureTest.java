package dev.yaks.testing.standard;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "dev.yaks.testing.standard" },
        plugin = { "dev.yaks.testing.report.TestReporter" } )
public class StandardFeatureTest {
}
