package io.github.luasgraphicss.minemod.verification.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.CallSite;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportLog;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.UnsupportedOperationReport;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportedConditionsTest {
    @Test
    void eachReportedOperationIsShownOnceWithItsClassOfSupport() {
        ReportLog reports = new ReportLog(4);
        reports.record(report("query result across native draws", OperationSupport.BRIDGE_REQUIRED));
        reports.record(report("query result across native draws", OperationSupport.BRIDGE_REQUIRED));
        reports.record(report("glDrawBuffer on the front buffer", OperationSupport.OUT_OF_CONTRACT));

        assertEquals(
                List.of(
                        "waiting for a bridge: query result across native draws",
                        "outside the contract: glDrawBuffer on the front buffer"),
                ReportedConditions.of(reports));
    }

    private static UnsupportedOperationReport report(String operation, OperationSupport support) {
        return UnsupportedOperationReport.of(operation, support, CallSite.unknown());
    }
}
