package io.github.luasgraphicss.minemod.compatibility.interop.handoff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushLog;
import io.github.luasgraphicss.minemod.compatibility.state.submission.SubmissionCheckpoints;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SharedAttachmentHandoffTest {
    private final RecordingSharingBackend backend = new RecordingSharingBackend();
    private final SharedAttachmentHandoff handoff = new SharedAttachmentHandoff(backend);

    @Test
    void aMixedFrameSignalsAndWaitsInTheOrderOfTheProtocol() {
        SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(handoff, new FlushLog(8));
        checkpoints.beginFrame(1);

        checkpoints.beforeNativePass();
        checkpoints.beforeProviderDraw();
        checkpoints.beforeNativePass();
        checkpoints.present();

        assertEquals(
                Arrays.asList(
                        RecordingSharingBackend.RENDERER_SIGNAL,
                        RecordingSharingBackend.PROVIDER_WAIT,
                        RecordingSharingBackend.PROVIDER_SIGNAL,
                        RecordingSharingBackend.RENDERER_WAIT),
                backend.steps());
        assertEquals(1, handoff.completedRoundTrips());
        assertFalse(handoff.hasPendingSignal());
    }

    @Test
    void aSynchronousReadFinishesTheProviderAfterTheHandover() {
        SubmissionCheckpoints checkpoints = new SubmissionCheckpoints(handoff, new FlushLog(8));
        checkpoints.beginFrame(1);

        checkpoints.beforeSynchronousRead();

        assertEquals(
                Arrays.asList(
                        RecordingSharingBackend.RENDERER_SIGNAL,
                        RecordingSharingBackend.PROVIDER_WAIT,
                        RecordingSharingBackend.PROVIDER_FINISH),
                backend.steps());
    }

    @Test
    void aFailedWaitIsRetriedWithoutSignalingTheSemaphoreAgain() {
        backend.failNextProviderWait();

        assertThrows(IllegalStateException.class, handoff::releaseToProvider);
        assertTrue(handoff.hasPendingSignal());

        handoff.releaseToProvider();

        assertEquals(
                Arrays.asList(RecordingSharingBackend.RENDERER_SIGNAL, RecordingSharingBackend.PROVIDER_WAIT),
                backend.steps());
        assertFalse(handoff.hasPendingSignal());
    }

    @Test
    void repeatedRoundTripsLeaveNoSignalBehind() {
        for (int trip = 0; trip < 1_000; trip++) {
            handoff.releaseToProvider();
            handoff.reclaimForRenderer();
        }

        assertEquals(1_000, handoff.completedRoundTrips());
        assertFalse(handoff.hasPendingSignal());
        assertEquals(4_000, backend.steps().size());
    }
}
