package org.citrusframework.yaks.standard;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * @author Christoph Deppisch
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        strict = true,
        glue = { "org.citrusframework.yaks.standard" },
        plugin = { "org.citrusframework.yaks.report.TestReporter" } )
public class StandardFeatureTest {
}
