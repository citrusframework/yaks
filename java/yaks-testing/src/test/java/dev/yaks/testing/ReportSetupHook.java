package dev.yaks.testing;

import cucumber.api.Scenario;
import cucumber.api.java.Before;

/**
 * @author Christoph Deppisch
 */
public class ReportSetupHook {

    @Before
    public void setup(Scenario scenario) {
        System.setProperty("yaks.termination.log", ReporterTest.TERMINATION_LOG);
    }
}
