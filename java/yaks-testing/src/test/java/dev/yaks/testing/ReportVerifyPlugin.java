package dev.yaks.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import cucumber.api.SummaryPrinter;
import cucumber.api.event.EventListener;
import cucumber.api.event.EventPublisher;
import cucumber.api.event.TestRunFinished;
import cucumber.api.formatter.Formatter;
import org.junit.Assert;

/**
 * @author Christoph Deppisch
 */
public class ReportVerifyPlugin implements SummaryPrinter, Formatter, EventListener {

    @Override
    public void setEventPublisher(EventPublisher eventPublisher) {
        eventPublisher.registerHandlerFor(TestRunFinished.class, event -> {
            try {
                Assert.assertTrue(Files.exists(Paths.get(ReporterTest.TERMINATION_LOG)));
                List<String> lines = Files.readAllLines(Paths.get(ReporterTest.TERMINATION_LOG));
                Assert.assertEquals(1, lines.size());
                Assert.assertEquals("dev/yaks/testing/report.feature:3 SUCCESS", lines.get(0));
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        });
    }
}
