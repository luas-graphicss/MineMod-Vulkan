package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.codec.BufferUsage;
import io.github.luasgraphicss.minemod.codec.CommandStreamWriter;
import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import io.github.luasgraphicss.minemod.contract.abi.AbiResult;
import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import io.github.luasgraphicss.minemod.contract.capability.Capability;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import io.github.luasgraphicss.minemod.contract.session.SessionCounters;
import io.github.luasgraphicss.minemod.contract.session.SessionLimits;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class NativeSessionTest {
    private static final SessionLimits LIMITS = new SessionLimits(8, 4096);
    private static final ResourceHandle BUFFER = ResourceHandle.of(ResourceKind.BUFFER, 1, 0);
    private static final ResourceHandle NEVER_CREATED = ResourceHandle.of(ResourceKind.BUFFER, 1, 5);
    private static final byte[] DATA = {1, 2, 3, 4, 5};
    private static final int BUFFER_BYTES = 16;
    private static final int STREAM_HEADER_BYTES = 16;
    private static final int TRANSPORT_BYTES = 256;

    @Test
    void keepsWrittenBytesAfterTheSubmittedBufferIsRecycled() {
        ByteBuffer transport = ByteBuffer.allocateDirect(TRANSPORT_BYTES);
        try (NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS)) {
            ByteBuffer stream = new CommandStreamWriter(transport)
                    .createBuffer(BUFFER, BUFFER_BYTES, EnumSet.of(BufferUsage.VERTEX))
                    .writeBuffer(BUFFER, 4, ByteBuffer.wrap(DATA))
                    .finish();
            assertEquals(StreamReport.accepted(2), session.submit(stream));

            while (transport.hasRemaining()) {
                transport.put((byte) 0xFF);
            }

            ByteBuffer contents = ByteBuffer.allocateDirect(DATA.length);
            session.readBuffer(BUFFER, 4, contents);
            assertArrayEquals(DATA, DirectStreams.bytesOf(contents));
        }
    }

    @Test
    void countsWhatEachSubmissionDidToTheSession() {
        try (NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS)) {
            session.submit(writer()
                    .createBuffer(BUFFER, BUFFER_BYTES, EnumSet.of(BufferUsage.UNIFORM))
                    .writeBuffer(BUFFER, 0, ByteBuffer.wrap(DATA))
                    .finish());
            session.submit(ByteBuffer.allocateDirect(STREAM_HEADER_BYTES));
            session.submit(writer().destroyBuffer(NEVER_CREATED).finish());

            SessionCounters counters = session.counters();
            assertEquals(1, counters.submissionsAccepted());
            assertEquals(1, counters.submissionsRejected());
            assertEquals(1, counters.submissionsAborted());
            assertEquals(2, counters.commandsExecuted());
            assertEquals(1, counters.buffersCreated());
            assertEquals(0, counters.buffersDestroyed());
            assertEquals(1, counters.liveBuffers());
            assertEquals(BUFFER_BYTES, counters.liveBufferBytes());
        }
    }

    @Test
    void reportsTheCommandThatUsedADestroyedBuffer() {
        try (NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS)) {
            session.submit(writer()
                    .createBuffer(BUFFER, BUFFER_BYTES, EnumSet.of(BufferUsage.INDEX))
                    .destroyBuffer(BUFFER)
                    .finish());

            StreamReport report = session.submit(writer().writeBuffer(BUFFER, 0, ByteBuffer.wrap(DATA)).finish());

            assertEquals(new StreamReport(AbiResult.STALE_HANDLE, 0, 0, STREAM_HEADER_BYTES), report);
            assertEquals(AbiResult.STALE_HANDLE, readFailure(session, BUFFER).result());
            assertEquals(AbiResult.INVALID_HANDLE, readFailure(session, NEVER_CREATED).result());
        }
    }

    @Test
    void refusesEveryCallAfterClose() {
        NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS);
        session.close();
        session.close();

        assertThrows(IllegalStateException.class, session::counters);
        assertThrows(IllegalStateException.class, () -> session.submit(writer().finish()));
        assertThrows(IllegalStateException.class, () -> session.readBuffer(BUFFER, 0, ByteBuffer.allocateDirect(1)));
    }

    @Test
    void refusesHeapBuffersBeforeReachingNativeCode() {
        try (NativeSession session = LoadedBridge.BRIDGE.openSession(LIMITS)) {
            assertThrows(IllegalArgumentException.class, () -> session.submit(ByteBuffer.allocate(STREAM_HEADER_BYTES)));
            assertThrows(IllegalArgumentException.class, () -> session.readBuffer(BUFFER, 0, ByteBuffer.allocate(1)));

            assertEquals(0, session.counters().submissionsRejected());
        }
    }

    @Test
    void opensNoSessionForAnIncompatibleAbiOrExcessiveLimits() {
        LoadedBridge.BRIDGE.negotiation();
        long capabilities = Capability.maskOf(EnumSet.allOf(Capability.class));
        AbiVersion otherAbi = new AbiVersion(AbiVersion.CURRENT.major() + 1, 0);
        SessionLimits excessiveLimits = new SessionLimits(Integer.MAX_VALUE, 1);

        AbiException incompatible =
                assertThrows(AbiException.class, () -> NativeSession.open(otherAbi, capabilities, LIMITS));
        AbiException excessive =
                assertThrows(AbiException.class, () -> LoadedBridge.BRIDGE.openSession(excessiveLimits));

        assertEquals(AbiResult.INCOMPATIBLE_ABI, incompatible.result());
        assertEquals(AbiResult.LIMIT_EXCEEDED, excessive.result());
    }

    private static CommandStreamWriter writer() {
        return new CommandStreamWriter(ByteBuffer.allocateDirect(TRANSPORT_BYTES));
    }

    private static AbiException readFailure(NativeSession session, ResourceHandle buffer) {
        return assertThrows(AbiException.class, () -> session.readBuffer(buffer, 0, ByteBuffer.allocateDirect(1)));
    }
}
