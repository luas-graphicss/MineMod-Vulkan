package io.github.luasgraphicss.minemod.compatibility.translation.query;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class QueryCalls {
    public static final long NO_RESULT = 0;

    private static final int GL_QUERY_RESULT = 0x8866;
    private static final int QUERY_ARGUMENT = 0;
    private static final int PARAMETER_ARGUMENT = 1;

    private final QuerySpans spans;
    private final ObjectNames names;
    private final SubmissionCheckpoints checkpoints;
    private final ProviderCalls provider;

    public QueryCalls(
            QuerySpans spans, ObjectNames names, SubmissionCheckpoints checkpoints, ProviderCalls provider) {
        this.spans = Objects.requireNonNull(spans, "spans");
        this.names = Objects.requireNonNull(names, "names");
        this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public void begin(String function, int target, int query) {
        int backing = names.backingForUse(ObjectKind.QUERY, query);
        if (backing == ObjectNames.REJECTED) {
            return;
        }
        spans.begin(target, query);
        provider.invoke(function, new long[] {target, backing});
    }

    public void end(String function, int target) {
        spans.end(target);
        provider.invoke(function, new long[] {target});
    }

    public void beginTransformFeedback(String function, int primitiveMode) {
        spans.beginTransformFeedback();
        provider.invoke(function, new long[] {primitiveMode});
    }

    public void endTransformFeedback(String function) {
        spans.end(QuerySpans.TRANSFORM_FEEDBACK);
        provider.invoke(function, new long[0]);
    }

    public long result(String function, long[] arguments) {
        int query = (int) arguments[QUERY_ARGUMENT];
        if (!spans.resultIsComplete(query)) {
            return NO_RESULT;
        }
        int backing = names.backingForUse(ObjectKind.QUERY, query);
        if (backing == ObjectNames.REJECTED) {
            return NO_RESULT;
        }
        if (arguments[PARAMETER_ARGUMENT] == GL_QUERY_RESULT) {
            checkpoints.beforeSynchronousRead();
        }
        long[] forwarded = arguments.clone();
        forwarded[QUERY_ARGUMENT] = backing;
        return provider.invoke(function, forwarded);
    }

    public void delete(int[] queries) {
        for (int query : queries) {
            spans.forget(query);
        }
        names.delete(ObjectKind.QUERY, queries);
    }
}
