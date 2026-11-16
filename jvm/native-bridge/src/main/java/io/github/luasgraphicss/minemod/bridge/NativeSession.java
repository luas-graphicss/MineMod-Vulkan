package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.session.SessionCounters;
import io.github.luasgraphicss.minemod.contract.session.SessionLimits;
import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;

public final class NativeSession implements AutoCloseable {
    private static final long CLOSED = 0;

    private long address;

    private NativeSession(long address) {
        this.address = address;
    }

    static NativeSession open(AbiVersion abiVersion, long requiredCapabilities, SessionLimits limits) {
        long[] address = new long[1];
        int resultCode = nativeCreate(
                abiVersion.major(),
                abiVersion.minor(),
                requiredCapabilities,
                limits.maxBuffers(),
                limits.maxBufferBytes(),
                address);
        NativeResults.requireOk(resultCode, "opening a session for ABI " + abiVersion + " with " + limits);
        return new NativeSession(address[0]);
    }

    public synchronized StreamReport submit(ByteBuffer stream) {
        DirectBuffers.requireDirect(stream, "stream");
        long[] fields = new long[StreamReportFields.COUNT];
        int resultCode = nativeSubmit(openAddress(), stream, stream.position(), stream.remaining(), fields);
        return StreamReportFields.decode(resultCode, fields);
    }

    public synchronized void readBuffer(ResourceHandle buffer, long offset, ByteBuffer destination) {
        DirectBuffers.requireDirect(destination, "destination");
        if (offset < 0) {
            throw new IllegalArgumentException("buffer offset must not be negative: " + offset);
        }
        int resultCode = nativeReadBuffer(
                openAddress(), buffer.bits(), offset, destination, destination.position(), destination.remaining());
        NativeResults.requireOk(resultCode, "reading " + destination.remaining() + " byte(s) of " + buffer + " at offset " + offset);
    }

    public synchronized SessionCounters counters() {
        long[] fields = new long[SessionCounterFields.COUNT];
        NativeResults.requireOk(nativeReadCounters(openAddress(), fields), "reading the session counters");
        return SessionCounterFields.decode(fields);
    }

    @Override
    public synchronized void close() {
        if (address == CLOSED) {
            return;
        }
        nativeDestroy(address);
        address = CLOSED;
    }

    private long openAddress() {
        if (address == CLOSED) {
            throw new IllegalStateException("the native session is closed");
        }
        return address;
    }

    private static native int nativeCreate(
            int abiMajor,
            int abiMinor,
            long requiredCapabilities,
            int maxBuffers,
            long maxBufferBytes,
            long[] sessionAddress);

    private static native void nativeDestroy(long session);

    private static native int nativeSubmit(
            long session, ByteBuffer stream, int offset, int length, long[] reportFields);

    private static native int nativeReadBuffer(
            long session, long buffer, long bufferOffset, ByteBuffer destination, int destinationOffset, int length);

    private static native int nativeReadCounters(long session, long[] counterFields);
}
