package io.github.luasgraphicss.minemod.compatibility.translation.section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.StateReconciler;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallObserver;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallTranslator;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.OpenConstructs;
import io.github.luasgraphicss.minemod.compatibility.translation.names.NameArguments;
import io.github.luasgraphicss.minemod.compatibility.translation.query.QuerySpans;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class MixedSectionsTest {
    private static final int BLEND = 0;
    private static final int DEPTH_TEST = 1;
    private static final long OFF = 0;
    private static final long ON = 1;
    private static final int GL_BLEND = 0x0BE2;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_QUADS = 0x0007;
    private static final int GL_SAMPLES_PASSED = 0x8914;
    private static final long[] TRIANGLE = {0x0004, 0, 3};

    private final LayerFixture fixture = new LayerFixture();
    private final FlushLog flushes = new FlushLog(16);
    private final SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(fixture.provider, flushes);
    private final List<String> adopted = new ArrayList<>();
    private final StateReconciler reconciler = new StateReconciler(
            new long[] {OFF, ON},
            new LeftoverPolicy[] {LeftoverPolicy.RESTORE, LeftoverPolicy.ADOPT},
            (slot, value) -> fixture.provider.invoke("apply state", new long[] {slot, value}),
            (slot, value) -> adopted.add(slot + "=" + value));
    private final OpenConstructs constructs = new OpenConstructs();
    private final QuerySpans spans = new QuerySpans(fixture.reporter, fixture.errors);
    private final MixedSections sections =
            new MixedSections(reconciler, checkpoints, spans, constructs, fixture.provider, fixture.reporter);
    private final CallTranslator translator = new CallTranslator(
            CallCatalog.standard(),
            new NameArguments(fixture.names(ContextProfile.COMPATIBILITY)),
            checkpoints,
            constructs,
            Collections.<CallObserver>singletonList(this::followDirectState),
            fixture.provider);

    MixedSectionsTest() {
        checkpoints.beginFrame(1);
    }

    @Test
    void aFrameAlternatesNativePassesWrapperStateAndDirectOpenGlInOrder() {
        sections.beforeNativePass();
        reconciler.setSemantic(BLEND, ON);

        sections.enterCallback();
        translator.call("glDisable", new long[] {GL_DEPTH_TEST});
        translator.call("glDisable", new long[] {GL_BLEND});
        translator.call("glDrawArrays", TRIANGLE);
        sections.exitCallback();

        sections.beforeNativePass();

        sections.enterCallback();
        translator.call("glDrawArrays", TRIANGLE);
        sections.exitCallback();

        sections.present();

        assertEquals(
                Arrays.asList(
                        "apply state[0, 1]",
                        "glDisable[2929]",
                        "glDisable[3042]",
                        RecordingProvider.RELEASE,
                        "glDrawArrays[4, 0, 3]",
                        RecordingProvider.RECLAIM,
                        "apply state[0, 1]",
                        RecordingProvider.RELEASE,
                        "glDrawArrays[4, 0, 3]",
                        RecordingProvider.RECLAIM),
                fixture.provider.events());
        assertEquals(Collections.singletonList("1=0"), adopted);
        assertEquals(ON, reconciler.semanticValue(BLEND));
        assertEquals(2, flushes.count(FlushReason.CALLBACK_NEEDS_ATTACHMENTS));
        assertEquals(1, flushes.count(FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS));
        assertEquals(1, flushes.count(FlushReason.PRESENTATION));
    }

    @Test
    void aFrameWithoutCallbackDrawsNeverLeavesTheRenderer() {
        sections.beforeNativePass();
        sections.enterCallback();
        translator.call("glDisable", new long[] {GL_BLEND});
        sections.exitCallback();
        sections.beforeNativePass();
        sections.present();

        assertEquals(Collections.singletonList("glDisable[3042]"), fixture.provider.events());
        assertEquals(0, flushes.recordedCount());
    }

    @Test
    void aPrimitiveLeftOpenIsClosedBeforeTheAttachmentsGoBack() {
        sections.enterCallback();
        translator.call("glBegin", new long[] {GL_QUADS});

        assertEquals(Collections.singletonList("a glBegin primitive was left open"), sections.exitCallback());

        sections.beforeNativePass();
        assertEquals(
                Arrays.asList(RecordingProvider.RELEASE, "glBegin[7]", "glEnd[]", RecordingProvider.RECLAIM),
                fixture.provider.events());
        assertEquals(OperationSupport.OUT_OF_CONTRACT, fixture.onlyReport().support());
    }

    @Test
    void nativeWorkNeverStartsInsideACallback() {
        sections.enterCallback();

        assertThrows(IllegalStateException.class, sections::beforeNativePass);
        assertThrows(IllegalStateException.class, sections::present);
    }

    @Test
    void aQueryLeftOpenAcrossANativePassHasNoResult() {
        sections.enterCallback();
        spans.begin(GL_SAMPLES_PASSED, 1);
        sections.exitCallback();

        sections.beforeNativePass();

        sections.enterCallback();
        spans.end(GL_SAMPLES_PASSED);
        sections.exitCallback();

        assertFalse(spans.resultIsComplete(1));
    }

    private void followDirectState(String function, long[] arguments) {
        if (!"glEnable".equals(function) && !"glDisable".equals(function)) {
            return;
        }
        long value = "glEnable".equals(function) ? ON : OFF;
        if (arguments[0] == GL_BLEND) {
            reconciler.observeDirect(BLEND, value);
        } else if (arguments[0] == GL_DEPTH_TEST) {
            reconciler.observeDirect(DEPTH_TEST, value);
        }
    }
}
