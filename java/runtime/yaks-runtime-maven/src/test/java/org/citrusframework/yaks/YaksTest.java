package org.citrusframework.yaks;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = {
                "com.consol.citrus.cucumber.step.runner.core",
                "org.citrusframework.yaks.http",
                "org.citrusframework.yaks.swagger",
                "org.citrusframework.yaks.camel",
                "org.citrusframework.yaks.jdbc",
                "org.citrusframework.yaks.standard",
        },
        plugin = { "org.citrusframework.yaks.report.TestReporter" }
)
public class YaksTest {
}
