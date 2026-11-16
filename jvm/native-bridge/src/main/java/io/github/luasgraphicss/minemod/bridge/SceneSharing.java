package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSemaphores;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;

public final class SceneSharing {
    private static final int SEMAPHORE_COUNT = 2;
    private static final int RENDERER_DONE = 0;
    private static final int PROVIDER_DONE = 1;

    private final NativeSceneSession session;

    SceneSharing(NativeSceneSession session) {
        this.session = session;
    }

    public SharedAttachments describe() {
        byte[] identity = new byte[SharingFields.IDENTITY_BYTES];
        long[] fields = new long[SharingFields.COUNT];
        NativeResults.requireOk(
                session.withOpenAddress(address -> nativeDescribe(address, identity, fields)),
                "describing the shared attachments");
        return SharingFields.decode(identity, fields);
    }

    public long exportMemory(SharedAttachment attachment) {
        long[] handle = new long[1];
        NativeResults.requireOk(
                session.withOpenAddress(address -> nativeExportAttachment(address, attachment.code(), handle)),
                "exporting the memory of the " + attachment + " attachment");
        return handle[0];
    }

    public HandoffSemaphores exportSemaphores() {
        long[] handles = new long[SEMAPHORE_COUNT];
        NativeResults.requireOk(
                session.withOpenAddress(address -> nativeExportSemaphores(address, handles)),
                "exporting the handoff semaphores");
        return new HandoffSemaphores(handles[RENDERER_DONE], handles[PROVIDER_DONE]);
    }

    public void releaseToProvider(HandoffSynchronization synchronization) {
        NativeResults.requireOk(
                session.withOpenAddress(address -> nativeRelease(address, synchronization.code())),
                "handing the frame over to the provider with a " + synchronization + " handoff");
    }

    public void reclaimForRenderer() {
        NativeResults.requireOk(
                session.withOpenAddress(SceneSharing::nativeReclaim), "taking the frame back from the provider");
    }

    private static native int nativeDescribe(long session, byte[] identity, long[] sharingFields);

    private static native int nativeExportAttachment(long session, int attachment, long[] handleField);

    private static native int nativeExportSemaphores(long session, long[] handleFields);

    private static native int nativeRelease(long session, int synchronization);

    private static native int nativeReclaim(long session);
}
