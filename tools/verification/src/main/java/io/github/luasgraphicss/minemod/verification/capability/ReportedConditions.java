package io.github.luasgraphicss.minemod.verification.capability;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportLog;
import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportedOperation;
import java.util.List;

public final class ReportedConditions {
    private ReportedConditions() {}

    public static List<String> of(ReportLog reports) {
        return reports.reportedOperations().stream()
                .map(ReportedOperation::firstReport)
                .map(report -> report.support().description() + ": " + report.operation())
                .toList();
    }
}
