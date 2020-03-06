package org.citrusframework.yaks.report;

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
                "org.citrusframework.yaks",
                "com.consol.citrus.cucumber.step.runner.core"
        },
        plugin = {
                "org.citrusframework.yaks.report.TestReporter",
                "org.citrusframework.yaks.report.ReportVerifyPlugin"
        }
)
public class ReporterTest {
}
