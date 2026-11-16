package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReportLog {
    private final int capacity;
    private final Map<ReportKey, Tally> tallies = new LinkedHashMap<>();
    private long droppedCount;

    public ReportLog(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("a report log needs room for at least one report: " + capacity);
        }
        this.capacity = capacity;
    }

    public synchronized void record(UnsupportedOperationReport report) {
        ReportKey key = new ReportKey(report);
        Tally tally = tallies.get(key);
        if (tally != null) {
            tally.occurrences++;
            return;
        }
        if (tallies.size() == capacity) {
            droppedCount++;
            return;
        }
        tallies.put(key, new Tally(report));
    }

    public synchronized List<ReportedOperation> reportedOperations() {
        List<ReportedOperation> reported = new ArrayList<>(tallies.size());
        for (Tally tally : tallies.values()) {
            reported.add(new ReportedOperation(tally.firstReport, tally.occurrences));
        }
        return reported;
    }

    public synchronized long droppedCount() {
        return droppedCount;
    }

    private static final class Tally {
        private final UnsupportedOperationReport firstReport;
        private long occurrences = 1;

        private Tally(UnsupportedOperationReport firstReport) {
            this.firstReport = firstReport;
        }
    }

    private static final class ReportKey {
        private final String operation;
        private final OperationSupport support;
        private final CallSite callSite;

        private ReportKey(UnsupportedOperationReport report) {
            this.operation = report.operation();
            this.support = report.support();
            this.callSite = report.callSite();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ReportKey)) {
                return false;
            }
            ReportKey key = (ReportKey) other;
            return operation.equals(key.operation) && support == key.support && callSite.equals(key.callSite);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operation, support, callSite);
        }
    }
}
