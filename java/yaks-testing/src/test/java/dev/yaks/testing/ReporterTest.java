package dev.yaks.testing;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "dev.yaks.testing", "com.consol.citrus.cucumber.step.runner.core" },
        plugin = { "dev.yaks.testing.TestReporter",
                   "dev.yaks.testing.ReportVerifyPlugin" } )
public class ReporterTest {
    static final String TERMINATION_LOG = "target/termination.log";
}
