package dev.yaks.testing;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = {
                "com.consol.citrus.cucumber.step.runner.core",
                "dev.yaks.testing.http",
                "dev.yaks.testing.swagger",
                "dev.yaks.testing.camel",
                "dev.yaks.testing.jdbc",
                "dev.yaks.testing.standard",
        },
        plugin = { "dev.yaks.testing.report.TestReporter" }
)
public class YaksTest {
}
