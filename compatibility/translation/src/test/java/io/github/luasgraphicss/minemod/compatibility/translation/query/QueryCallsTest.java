package io.github.luasgraphicss.minemod.compatibility.translation.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class QueryCallsTest {
    private static final int GL_SAMPLES_PASSED = 0x8914;
    private static final int GL_QUERY_RESULT = 0x8866;
    private static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final long RESULT_ADDRESS = 0x1000;

    private final LayerFixture fixture = new LayerFixture();
    private final ObjectNames names = fixture.names(ContextProfile.CORE);
    private final SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(fixture.provider, new FlushLog(8));
    private final QuerySpans spans = new QuerySpans(fixture.reporter, fixture.errors);
    private final QueryCalls queries = new QueryCalls(spans, names, checkpoints, fixture.provider);
    private final int query = names.generate(ObjectKind.QUERY, 1)[0];

    QueryCallsTest() {
        checkpoints.beginFrame(1);
    }

    @Test
    void aQueryOfProviderDrawsIsForwardedUnderTheProvidersObject() {
        queries.begin("glBeginQuery", GL_SAMPLES_PASSED, query);
        queries.end("glEndQuery", GL_SAMPLES_PASSED);

        assertEquals(
                Arrays.asList("glBeginQuery[35092, " + RecordingProvider.FIRST_BACKING + "]", "glEndQuery[35092]"),
                fixture.provider.events());
    }

    @Test
    void askingWhetherAResultIsAvailableNeverWaits() {
        result(GL_QUERY_RESULT_AVAILABLE);

        assertEquals(
                Collections.singletonList("glGetQueryObjectiv[" + RecordingProvider.FIRST_BACKING + ", 34919, 4096]"),
                fixture.provider.events());
    }

    @Test
    void readingTheResultWaitsForTheProvider() {
        result(GL_QUERY_RESULT);

        assertEquals(
                Arrays.asList(
                        RecordingProvider.RELEASE,
                        RecordingProvider.FINISH,
                        "glGetQueryObjectiv[" + RecordingProvider.FIRST_BACKING + ", 34918, 4096]"),
                fixture.provider.events());
    }

    @Test
    void aResultThatMissedNativeDrawsIsNeverReadFromTheProvider() {
        queries.begin("glBeginQuery", GL_SAMPLES_PASSED, query);
        spans.nativeWorkRecorded();
        queries.end("glEndQuery", GL_SAMPLES_PASSED);

        assertEquals(QueryCalls.NO_RESULT, result(GL_QUERY_RESULT));

        assertEquals(2, fixture.provider.events().size());
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
    }

    @Test
    void aDeletedQueryLosesItsIncompleteMarkWithItsName() {
        queries.begin("glBeginQuery", GL_SAMPLES_PASSED, query);
        spans.nativeWorkRecorded();
        queries.end("glEndQuery", GL_SAMPLES_PASSED);

        queries.delete(new int[] {query});

        assertEquals(query, names.generate(ObjectKind.QUERY, 1)[0]);
        result(GL_QUERY_RESULT_AVAILABLE);
        assertEquals(0, fixture.errors.take());
    }

    @Test
    void transformFeedbackIsForwardedAndItsSpanFollowed() {
        queries.beginTransformFeedback("glBeginTransformFeedback", GL_TRIANGLES);
        spans.nativeWorkRecorded();
        queries.endTransformFeedback("glEndTransformFeedback");

        assertEquals(
                Arrays.asList("glBeginTransformFeedback[4]", "glEndTransformFeedback[]"), fixture.provider.events());
        assertEquals("transform feedback across native draws", fixture.onlyReport().operation());
    }

    private long result(int parameter) {
        return queries.result("glGetQueryObjectiv", new long[] {query, parameter, RESULT_ADDRESS});
    }
}
