package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportLogTest {
    private static final CallSite OVERLAY = new CallSite("com.example.mod.Overlay", "render", "example.jar");
    private static final CallSite MINIMAP = new CallSite("com.example.map.Minimap", "draw", "minimap.jar");

    @Test
    void countsRepeatedReportsFromOneCallSiteAsOneEntry() {
        ReportLog log = new ReportLog(4);

        log.record(feedbackFrom(OVERLAY));
        log.record(feedbackFrom(OVERLAY));
        log.record(feedbackFrom(OVERLAY));

        List<ReportedOperation> reported = log.reportedOperations();
        assertEquals(1, reported.size());
        assertEquals(3, reported.get(0).occurrences());
    }

    @Test
    void keepsDifferentCallSitesAndClassificationsApart() {
        ReportLog log = new ReportLog(4);

        log.record(feedbackFrom(OVERLAY));
        log.record(feedbackFrom(MINIMAP));
        log.record(UnsupportedOperationReport.of("glBeginTransformFeedback", OperationSupport.UNVERIFIED, OVERLAY));

        assertEquals(3, log.reportedOperations().size());
    }

    @Test
    void countsWhatDoesNotFitInsteadOfGrowing() {
        ReportLog log = new ReportLog(1);

        log.record(feedbackFrom(OVERLAY));
        log.record(feedbackFrom(MINIMAP));
        log.record(feedbackFrom(MINIMAP));

        assertEquals(1, log.reportedOperations().size());
        assertEquals(2, log.droppedCount());
    }

    @Test
    void describesTheOperationItsCallSiteAndItsResource() {
        ResourceContext defaultFramebuffer = new ResourceContext(ObjectKind.FRAMEBUFFER, 0);
        UnsupportedOperationReport report = UnsupportedOperationReport.onResource(
                "glDrawBuffer", OperationSupport.OUT_OF_CONTRACT, OVERLAY, defaultFramebuffer);

        assertEquals(
                "glDrawBuffer is outside the contract, called from com.example.mod.Overlay.render (example.jar)"
                        + " on FRAMEBUFFER 0",
                report.toString());
    }

    @Test
    void refusesToReportASupportedOperation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UnsupportedOperationReport.of("glClear", OperationSupport.SUPPORTED, OVERLAY));
    }

    private static UnsupportedOperationReport feedbackFrom(CallSite callSite) {
        return UnsupportedOperationReport.of("glBeginTransformFeedback", OperationSupport.BRIDGE_REQUIRED, callSite);
    }
}
