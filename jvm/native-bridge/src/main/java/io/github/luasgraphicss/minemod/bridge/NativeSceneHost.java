package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.abi.AbiVersion;
import java.util.List;

public final class NativeSceneHost implements AutoCloseable {
    private static final long CLOSED = 0;

    private long address;

    private NativeSceneHost(long address) {
        this.address = address;
    }

    static NativeSceneHost open(AbiVersion abiVersion, boolean validates, List<String> windowSystemExtensions) {
        long[] address = new long[1];
        int resultCode = nativeCreate(
                abiVersion.major(), abiVersion.minor(), validates, windowSystemExtensions.toArray(new String[0]), address);
        NativeResults.requireOk(
                resultCode, "opening a scene host for ABI " + abiVersion + " with extensions " + windowSystemExtensions);
        return new NativeSceneHost(address[0]);
    }

    public synchronized long vulkanInstance() {
        return nativeVulkanInstance(openAddress());
    }

    public synchronized String validationText() {
        return nativeValidationText(openAddress());
    }

    public synchronized NativeSceneSession openSession(SceneSessionSetup setup) {
        return NativeSceneSession.open(openAddress(), setup);
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
            throw new IllegalStateException("the scene host is closed");
        }
        return address;
    }

    private static native int nativeCreate(
            int abiMajor, int abiMinor, boolean validates, String[] windowSystemExtensions, long[] hostAddress);

    private static native void nativeDestroy(long host);

    private static native long nativeVulkanInstance(long host);

    private static native String nativeValidationText(long host);
}
