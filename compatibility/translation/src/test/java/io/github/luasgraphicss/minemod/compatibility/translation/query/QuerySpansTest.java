package io.github.luasgraphicss.minemod.compatibility.translation.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import org.junit.jupiter.api.Test;

class QuerySpansTest {
    private static final int GL_SAMPLES_PASSED = 0x8914;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int QUERY = 3;

    private final LayerFixture fixture = new LayerFixture();
    private final QuerySpans spans = new QuerySpans(fixture.reporter, fixture.errors);

    @Test
    void aQueryThatOnlySawProviderDrawsHasACompleteResult() {
        spans.begin(GL_SAMPLES_PASSED, QUERY);
        spans.end(GL_SAMPLES_PASSED);
        spans.nativeWorkRecorded();

        assertTrue(spans.resultIsComplete(QUERY));
        assertEquals(0, fixture.reports.reportedOperations().size());
    }

    @Test
    void aQueryAcrossNativeDrawsAsksForABridgeInsteadOfAnswering() {
        spans.begin(GL_SAMPLES_PASSED, QUERY);
        spans.nativeWorkRecorded();
        spans.end(GL_SAMPLES_PASSED);

        assertFalse(spans.resultIsComplete(QUERY));

        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
        assertEquals(OperationSupport.BRIDGE_REQUIRED, fixture.onlyReport().support());
        assertEquals(ObjectKind.QUERY, fixture.onlyReport().resource().get().kind());
        assertEquals(QUERY, fixture.onlyReport().resource().get().name());
    }

    @Test
    void aQueryUsedAgainStartsClean() {
        spans.begin(GL_SAMPLES_PASSED, QUERY);
        spans.nativeWorkRecorded();
        spans.end(GL_SAMPLES_PASSED);

        spans.begin(GL_SAMPLES_PASSED, QUERY);
        spans.end(GL_SAMPLES_PASSED);

        assertTrue(spans.resultIsComplete(QUERY));
    }

    @Test
    void transformFeedbackAcrossNativeDrawsIsReportedWhenItEnds() {
        spans.beginTransformFeedback();
        assertEquals(1, spans.openCount());
        spans.nativeWorkRecorded();

        spans.end(QuerySpans.TRANSFORM_FEEDBACK);

        assertEquals("transform feedback across native draws", fixture.onlyReport().operation());
        assertEquals(OperationSupport.BRIDGE_REQUIRED, fixture.onlyReport().support());
        assertEquals(0, spans.openCount());
    }

    @Test
    void aDeletedQueryIsForgotten() {
        spans.begin(GL_SAMPLES_PASSED, QUERY);
        spans.nativeWorkRecorded();
        spans.end(GL_SAMPLES_PASSED);

        spans.forget(QUERY);

        assertTrue(spans.resultIsComplete(QUERY));
    }
}
