package dev.yaks.testing.report;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = {
                "dev.yaks.testing",
                "com.consol.citrus.cucumber.step.runner.core"
        },
        plugin = {
                "dev.yaks.testing.report.TestReporter",
                "dev.yaks.testing.report.ReportVerifyPlugin"
        }
)
public class ReporterTest {
}
