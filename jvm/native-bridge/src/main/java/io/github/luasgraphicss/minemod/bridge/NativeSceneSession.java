package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.stream.StreamReport;
import java.nio.ByteBuffer;
import java.util.function.LongFunction;

public final class NativeSceneSession implements AutoCloseable {
    private static final long CLOSED = 0;

    private long address;

    private NativeSceneSession(long address) {
        this.address = address;
    }

    static NativeSceneSession open(long host, SceneSessionSetup setup) {
        long[] address = new long[1];
        int resultCode = nativeCreate(
                host,
                setup.width(),
                setup.height(),
                setup.maxTextures(),
                setup.maxSections(),
                setup.sharesAttachments(),
                setup.windowSurface(),
                setup.uploadBytesPerFrame(),
                setup.geometryBytesPerFrame(),
                address);
        NativeResults.requireOk(resultCode, "opening a scene session of " + setup);
        return new NativeSceneSession(address[0]);
    }

    public synchronized StreamReport submit(ByteBuffer stream) {
        DirectBuffers.requireDirect(stream, "stream");
        long[] fields = new long[StreamReportFields.COUNT];
        int resultCode = nativeSubmit(openAddress(), stream, stream.position(), stream.remaining(), fields);
        return StreamReportFields.decode(resultCode, fields);
    }

    public synchronized void resize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("a frame extent cannot be negative: " + width + "x" + height);
        }
        NativeResults.requireOk(
                nativeResize(openAddress(), width, height), "resizing the scene session to " + width + "x" + height);
    }

    public synchronized void setPacing(PresentPacing pacing) {
        NativeResults.requireOk(nativeSetPacing(openAddress(), pacing.code()), "pacing the presentation by " + pacing);
    }

    public synchronized PresentationState presentationState() {
        long[] state = new long[1];
        NativeResults.requireOk(nativeReadPresentationState(openAddress(), state), "reading the presentation state");
        return PresentationState.fromCode(state[0]);
    }

    public synchronized void readFrame(ByteBuffer pixels) {
        DirectBuffers.requireDirect(pixels, "pixel");
        int resultCode = nativeReadFrame(openAddress(), pixels, pixels.position(), pixels.remaining());
        NativeResults.requireOk(resultCode, "reading the last frame into " + pixels.remaining() + " byte(s)");
    }

    public synchronized SceneCounters counters() {
        long[] fields = new long[SceneCounters.FIELD_COUNT];
        NativeResults.requireOk(nativeReadCounters(openAddress(), fields), "reading the scene counters");
        return new SceneCounters(fields);
    }

    public SceneReload reload() {
        return new SceneReload(this);
    }

    public SceneDiagnostics diagnostics() {
        return new SceneDiagnostics(this);
    }

    public SceneSharing sharing() {
        return new SceneSharing(this);
    }

    @Override
    public synchronized void close() {
        if (address == CLOSED) {
            return;
        }
        nativeDestroy(address);
        address = CLOSED;
    }

    synchronized <T> T withOpenAddress(LongFunction<T> call) {
        return call.apply(openAddress());
    }

    private long openAddress() {
        if (address == CLOSED) {
            throw new IllegalStateException("the scene session is closed");
        }
        return address;
    }

    private static native int nativeCreate(
            long host,
            int width,
            int height,
            int maxTextures,
            int maxSections,
            boolean sharesAttachments,
            long windowSurface,
            long uploadBytesPerFrame,
            long geometryBytesPerFrame,
            long[] sessionAddress);

    private static native void nativeDestroy(long session);

    private static native int nativeSubmit(
            long session, ByteBuffer stream, int offset, int length, long[] reportFields);

    private static native int nativeResize(long session, int width, int height);

    private static native int nativeSetPacing(long session, int pacing);

    private static native int nativeReadPresentationState(long session, long[] stateField);

    private static native int nativeReadFrame(long session, ByteBuffer pixels, int offset, int length);

    private static native int nativeReadCounters(long session, long[] counterFields);
}
