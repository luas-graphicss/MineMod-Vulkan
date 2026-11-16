package io.github.luasgraphicss.minemod.compatibility.translation.support;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectScope;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.CallSiteLocator;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportLog;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.UnsupportedOperationReport;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameSpace;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.Collections;
import java.util.Optional;

public final class LayerFixture {
    public static final int NAMES_PER_KIND = 16;

    private static final String LAYER_PACKAGE = "io.github.luasgraphicss.minemod.compatibility.translation.";

    public final RecordingProvider provider = new RecordingProvider();
    public final ContextNames contextNames = new ContextNames(
            new NameSpace(ObjectScope.SHARE_GROUP, NAMES_PER_KIND), new NameSpace(ObjectScope.CONTEXT, NAMES_PER_KIND));
    public final ErrorLedger errors = new ErrorLedger(provider);
    public final ReportLog reports = new ReportLog(NAMES_PER_KIND);
    public final OperationReporter reporter = new OperationReporter(
            reports, new CallSiteLocator(Collections.singletonList(LAYER_PACKAGE), className -> Optional.empty()));
    public long frame = 1;

    public ObjectNames names(ContextProfile profile) {
        return new ObjectNames(() -> contextNames, provider, errors, () -> frame, profile);
    }

    public UnsupportedOperationReport onlyReport() {
        if (reports.reportedOperations().size() != 1) {
            throw new AssertionError("expected one report, found " + reports.reportedOperations());
        }
        return reports.reportedOperations().get(0).firstReport();
    }
}
