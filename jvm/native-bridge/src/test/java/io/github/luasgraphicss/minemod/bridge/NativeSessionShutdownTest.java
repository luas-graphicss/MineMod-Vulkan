package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.session.SessionCounters;
import io.github.luasgraphicss.minemod.contract.session.SessionLimits;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class NativeSessionShutdownTest {
    private static final SessionLimits LIMITS = new SessionLimits(4, 1024);
    private static final long TIMEOUT_SECONDS = 30;
    private static final int STREAM_HEADER_BYTES = 16;

    @Test
    void closingWhileAnotherThreadSubmitsStopsThatThreadWithAnError() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS);
            CountDownLatch submitting = new CountDownLatch(1);
            Future<Long> submissions = executor.submit(() -> submitUntilClosed(session, submitting));

            assertTrue(submitting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            session.close();

            assertTrue(submissions.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) > 0);
            assertThrows(IllegalStateException.class, session::counters);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void aCancelledSubmitterLeavesTheSessionConsistent() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS)) {
            CountDownLatch submitting = new CountDownLatch(1);
            Future<?> submitter = executor.submit(() -> submitUntilInterrupted(session, submitting));

            assertTrue(submitting.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            submitter.cancel(true);
            executor.shutdown();
            assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            assertEquals(StreamReport.accepted(0), session.submit(emptyStream()));
            SessionCounters counters = session.counters();
            assertTrue(counters.submissionsAccepted() > 1);
            assertEquals(0, counters.submissionsRejected() + counters.submissionsAborted());
        } finally {
            executor.shutdownNow();
        }
    }

    private static long submitUntilClosed(NativeSession session, CountDownLatch submitting) {
        ByteBuffer stream = emptyStream();
        long submissions = 0;
        try {
            while (true) {
                session.submit(stream);
                submissions++;
                submitting.countDown();
            }
        } catch (IllegalStateException closed) {
            return submissions;
        }
    }

    private static void submitUntilInterrupted(NativeSession session, CountDownLatch submitting) {
        ByteBuffer stream = emptyStream();
        while (!Thread.currentThread().isInterrupted()) {
            session.submit(stream);
            submitting.countDown();
        }
    }

    private static ByteBuffer emptyStream() {
        return new CommandStreamWriter(ByteBuffer.allocateDirect(STREAM_HEADER_BYTES)).finish();
    }
}
