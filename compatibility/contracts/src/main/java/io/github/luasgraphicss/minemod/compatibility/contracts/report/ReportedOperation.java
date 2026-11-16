package io.github.luasgraphicss.minemod.compatibility.contracts.report;

public final class ReportedOperation {
    private final UnsupportedOperationReport firstReport;
    private final long occurrences;

    ReportedOperation(UnsupportedOperationReport firstReport, long occurrences) {
        this.firstReport = firstReport;
        this.occurrences = occurrences;
    }

    public UnsupportedOperationReport firstReport() {
        return firstReport;
    }

    public long occurrences() {
        return occurrences;
    }

    @Override
    public String toString() {
        return firstReport + ", seen " + occurrences + " time(s)";
    }
}
