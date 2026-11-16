package io.github.luasgraphicss.minemod.compatibility.translation.query;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuerySpans {
    public static final int TRANSFORM_FEEDBACK = 0x8E22;

    private static final int NO_QUERY = 0;
    private static final int GL_INVALID_OPERATION = 0x0502;

    private final OperationReporter reporter;
    private final ErrorLedger errors;
    private final Map<Integer, Span> openByTarget = new HashMap<>();
    private final Set<Integer> incompleteQueries = new HashSet<>();
    private long nativeWorkCount;

    public QuerySpans(OperationReporter reporter, ErrorLedger errors) {
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.errors = Objects.requireNonNull(errors, "errors");
    }

    public void begin(int target, int query) {
        openByTarget.put(target, new Span(query, nativeWorkCount));
        incompleteQueries.remove(query);
    }

    public void beginTransformFeedback() {
        begin(TRANSFORM_FEEDBACK, NO_QUERY);
    }

    public void end(int target) {
        Span span = openByTarget.remove(target);
        if (span == null || span.startedAt == nativeWorkCount) {
            return;
        }
        if (span.query == NO_QUERY) {
            reporter.report("transform feedback across native draws", OperationSupport.BRIDGE_REQUIRED);
        } else {
            incompleteQueries.add(span.query);
        }
    }

    public void nativeWorkRecorded() {
        nativeWorkCount++;
    }

    public boolean resultIsComplete(int query) {
        if (!incompleteQueries.contains(query)) {
            return true;
        }
        reporter.reportOnObject(
                "query result across native draws", OperationSupport.BRIDGE_REQUIRED, ObjectKind.QUERY, query);
        errors.raise(GL_INVALID_OPERATION);
        return false;
    }

    public void forget(int query) {
        incompleteQueries.remove(query);
    }

    public int openCount() {
        return openByTarget.size();
    }

    private static final class Span {
        private final int query;
        private final long startedAt;

        private Span(int query, long startedAt) {
            this.query = query;
            this.startedAt = startedAt;
        }
    }
}
