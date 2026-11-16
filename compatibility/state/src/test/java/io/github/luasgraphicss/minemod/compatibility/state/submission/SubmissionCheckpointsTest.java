package io.github.luasgraphicss.minemod.compatibility.state.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class SubmissionCheckpointsTest {
    private final RecordingHandoff handoff = new RecordingHandoff();
    private final FlushLog log = new FlushLog(16);
    private final SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(handoff, log);

    @Test
    void aCallbackDrawAfterNativeWorkHandsTheAttachmentsOver() {
        checkpoints.beginFrame(1);
        checkpoints.beforeNativePass();

        checkpoints.beforeProviderDraw();

        assertEquals(Collections.singletonList(RecordingHandoff.RELEASE), handoff.calls());
        assertEquals(AttachmentOwner.PROVIDER, checkpoints.owner());
        assertEquals(
                Collections.singletonList(new FlushRecord(1, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS)),
                log.recent());
    }

    @Test
    void consecutiveUsesByTheSameSideFlushOnce() {
        checkpoints.beginFrame(1);

        checkpoints.beforeProviderDraw();
        checkpoints.beforeProviderDraw();
        checkpoints.beforeNativePass();
        checkpoints.beforeNativePass();

        assertEquals(Arrays.asList(RecordingHandoff.RELEASE, RecordingHandoff.RECLAIM), handoff.calls());
        assertEquals(2, log.recordedCount());
    }

    @Test
    void alternatingSidesKeepsTheirOrderInTheLog() {
        checkpoints.beginFrame(1);

        checkpoints.beforeProviderDraw();
        checkpoints.beforeNativePass();
        checkpoints.beforeProviderDraw();

        assertEquals(
                Arrays.asList(
                        new FlushRecord(1, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS),
                        new FlushRecord(1, 1, FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS),
                        new FlushRecord(1, 2, FlushReason.CALLBACK_NEEDS_ATTACHMENTS)),
                log.recent());
    }

    @Test
    void aFrameWithoutCallbackDrawsNeverFlushes() {
        checkpoints.beginFrame(1);
        checkpoints.beforeNativePass();
        checkpoints.beforeNativePass();

        checkpoints.present();

        assertEquals(Collections.emptyList(), handoff.calls());
        assertEquals(0, log.recordedCount());
    }

    @Test
    void aSynchronousReadWaitsForTheProviderEveryTime() {
        checkpoints.beginFrame(1);

        checkpoints.beforeSynchronousRead();
        checkpoints.beforeSynchronousRead();

        assertEquals(
                Arrays.asList(RecordingHandoff.RELEASE, RecordingHandoff.FINISH, RecordingHandoff.FINISH),
                handoff.calls());
        assertEquals(1, log.count(FlushReason.CALLBACK_NEEDS_ATTACHMENTS));
        assertEquals(2, log.count(FlushReason.SYNCHRONOUS_READ));
    }

    @Test
    void presentingTakesTheAttachmentsBackAndClosesTheFrame() {
        checkpoints.beginFrame(1);
        checkpoints.beforeProviderDraw();

        checkpoints.present();

        assertEquals(Arrays.asList(RecordingHandoff.RELEASE, RecordingHandoff.RECLAIM), handoff.calls());
        assertEquals(AttachmentOwner.RENDERER, checkpoints.owner());
        assertEquals(1, log.count(FlushReason.PRESENTATION));
        assertThrows(IllegalStateException.class, checkpoints::present);
    }

    @Test
    void checkpointsRestartWithEachFrame() {
        checkpoints.beginFrame(1);
        checkpoints.beforeProviderDraw();
        checkpoints.present();

        checkpoints.beginFrame(2);
        checkpoints.beforeProviderDraw();

        assertEquals(new FlushRecord(2, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS), log.recent().get(2));
    }

    @Test
    void framesArePresentedBeforeTheNextOneAndNeverGoBack() {
        checkpoints.beginFrame(3);

        assertThrows(IllegalStateException.class, () -> checkpoints.beginFrame(4));
        checkpoints.present();
        assertThrows(IllegalArgumentException.class, () -> checkpoints.beginFrame(3));
    }

    @Test
    void aRefusedHandoverLeavesTheRendererAsOwner() {
        handoff.refuseRelease();
        checkpoints.beginFrame(1);

        assertThrows(IllegalStateException.class, checkpoints::beforeProviderDraw);
        assertEquals(AttachmentOwner.RENDERER, checkpoints.owner());
        assertEquals(0, log.recordedCount());
    }

    @Test
    void attachmentsAreOnlyUsedInsideAFrame() {
        assertThrows(IllegalStateException.class, checkpoints::beforeNativePass);
        assertThrows(IllegalStateException.class, checkpoints::beforeProviderDraw);
        assertThrows(IllegalStateException.class, checkpoints::beforeSynchronousRead);
    }
}
