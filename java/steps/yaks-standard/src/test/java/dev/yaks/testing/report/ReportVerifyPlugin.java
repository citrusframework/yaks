package dev.yaks.testing.report;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import cucumber.api.SummaryPrinter;
import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestRunFinished;
import cucumber.api.formatter.Formatter;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christoph Deppisch
 */
public class ReportVerifyPlugin implements SummaryPrinter, Formatter, EventListener {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(ReportVerifyPlugin.class);

    @Override
    public void setEventPublisher(EventPublisher eventPublisher) {
        eventPublisher.registerHandlerFor(TestRunFinished.class, event -> {
            try {
                Assert.assertTrue("Verify termination log exists", Files.exists(TestReporter.getTerminationLog()));
                List<String> lines = Files.readAllLines(TestReporter.getTerminationLog());
                Assert.assertTrue("Missing successful test result in termination log",
                        lines.contains("dev/yaks/testing/report/report.feature:3 SUCCESS"));
            } catch (IOException e) {
                LOG.warn("Failed to verify termination logs", e);
                Assert.fail(e.getMessage());
            }
        });
    }
}
