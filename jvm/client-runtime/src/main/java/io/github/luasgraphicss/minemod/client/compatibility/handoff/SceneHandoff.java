package io.github.luasgraphicss.minemod.client.compatibility.handoff;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.client.compatibility.sharing.ImportedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.handoff.SharedAttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.BlockingSharingBackend;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class SceneHandoff implements AttachmentHandoff, AutoCloseable {
    private final RecordedFrame frame;
    private final HandoffSource source;
    private AttachmentHandoff current;
    private boolean isWithProvider;

    private SceneHandoff(RecordedFrame frame, HandoffSource source, ImportedAttachments imported) {
        this.frame = frame;
        this.source = source;
        this.current = source.over(imported);
    }

    public static SceneHandoff open(
            RecordedFrame frame,
            SceneSharing sharing,
            ProviderCalls provider,
            NativeMemory memory,
            ImportedAttachments imported) {
        Objects.requireNonNull(frame, "frame");
        HandoffSynchronization synchronization = imported.plan().synchronization();
        SessionHandoff renderer = new SessionHandoff(sharing, synchronization);
        if (synchronization == HandoffSynchronization.SEMAPHORES) {
            return new SceneHandoff(frame, new SemaphoreHandoffSource(sharing, renderer, provider, memory), imported);
        }
        AttachmentHandoff blocking = new SharedAttachmentHandoff(new BlockingSharingBackend(renderer, provider));
        return new SceneHandoff(frame, attachments -> blocking, imported);
    }

    public void retarget(ImportedAttachments imported) {
        if (isWithProvider) {
            throw new IllegalStateException("the provider still holds the attachments that were replaced");
        }
        current = source.over(imported);
    }

    @Override
    public void releaseToProvider() {
        if (!frame.submitPart()) {
            return;
        }
        current.releaseToProvider();
        isWithProvider = true;
    }

    @Override
    public void reclaimForRenderer() {
        if (!isWithProvider) {
            return;
        }
        current.reclaimForRenderer();
        isWithProvider = false;
    }

    @Override
    public void finishProviderWork() {
        current.finishProviderWork();
    }

    @Override
    public void close() {
        source.close();
    }
}
