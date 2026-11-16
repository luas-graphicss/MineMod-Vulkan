package io.github.luasgraphicss.minemod.compatibility.translation.report;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.CallSite;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.CallSiteLocator;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportLog;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ResourceContext;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.UnsupportedOperationReport;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import java.util.Objects;

public final class OperationReporter {
    private final ReportLog log;
    private final CallSiteLocator locator;

    public OperationReporter(ReportLog log, CallSiteLocator locator) {
        this.log = Objects.requireNonNull(log, "log");
        this.locator = Objects.requireNonNull(locator, "locator");
    }

    public void report(String operation, OperationSupport support) {
        log.record(UnsupportedOperationReport.of(operation, support, callSite()));
    }

    public void reportOnObject(String operation, OperationSupport support, ObjectKind kind, int name) {
        log.record(UnsupportedOperationReport.onResource(
                operation, support, callSite(), new ResourceContext(kind, name)));
    }

    private CallSite callSite() {
        return locator.locate(new Throwable().getStackTrace());
    }
}
