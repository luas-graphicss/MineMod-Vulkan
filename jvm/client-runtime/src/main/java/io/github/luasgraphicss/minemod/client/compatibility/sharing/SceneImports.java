package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleLedger;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResource;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResourcePool;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.provider.ProviderExtensions;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.ProviderDevice;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.ProviderMemoryImports;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.SplitResourceBackend;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.List;
import java.util.Objects;

public final class SceneImports implements AutoCloseable {
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final long NEVER_USED = Long.MIN_VALUE;
    private static final long EVERY_FRAME = Long.MAX_VALUE;

    private final SceneSharing sharing;
    private final ProviderCalls provider;
    private final ScratchInts scratch;
    private final SessionAttachments attachments;
    private final ProviderMemoryImports imports;
    private final SharedResourcePool pool;
    private final DeviceIdentity providerDevice;
    private final List<String> providerExtensions;
    private ImportedAttachments current;

    public SceneImports(SceneSharing sharing, ProviderCalls provider, NativeMemory memory) {
        this.sharing = Objects.requireNonNull(sharing, "sharing");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerDevice = ProviderDevice.identify(provider, memory);
        this.providerExtensions = ProviderExtensions.list(provider, memory);
        this.attachments = new SessionAttachments(sharing);
        this.scratch = new ScratchInts(memory, 1);
        this.imports = new ProviderMemoryImports(
                provider, scratch, new ErrorLedger(() -> (int) provider.invoke("glGetError", NO_ARGUMENTS)));
        this.pool = new SharedResourcePool(
                new SplitResourceBackend(attachments, imports),
                new HandleLedger(SceneNegotiation.PLATFORM, sharing::closeExported));
        try {
            this.current = importAttachments();
        } catch (RuntimeException failure) {
            scratch.close();
            throw failure;
        }
    }

    public ImportedAttachments current() {
        return current;
    }

    public boolean followSession(long lastFrameOnTheReplaced) {
        if (sharing.describe().generation() == current.shared().generation()) {
            return false;
        }
        ImportedAttachments replaced = current;
        current = importAttachments();
        pool.retire(replaced.color(), lastFrameOnTheReplaced);
        pool.retire(replaced.depthStencil(), lastFrameOnTheReplaced);
        return true;
    }

    public int collect(long completedFrame) {
        return pool.collect(completedFrame);
    }

    public int liveImports() {
        return imports.liveCount();
    }

    public int liveAttachments() {
        return attachments.liveCount();
    }

    public int retiringCount() {
        return pool.retiringCount();
    }

    @Override
    public void close() {
        pool.retire(current.color(), NEVER_USED);
        pool.retire(current.depthStencil(), NEVER_USED);
        pool.collect(EVERY_FRAME);
        scratch.close();
    }

    private ImportedAttachments importAttachments() {
        SharedAttachments shared = sharing.describe();
        SharingPlan plan = SceneNegotiation.negotiate(shared, providerDevice, providerExtensions);
        int boundTexture = boundTexture();
        try {
            SharedResource color = pool.create(plan.share(SharedAttachment.COLOR));
            SharedResource depthStencil = createBeside(color, plan.share(SharedAttachment.DEPTH_STENCIL));
            return new ImportedAttachments(shared, plan, color, depthStencil);
        } finally {
            provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, boundTexture});
        }
    }

    private SharedResource createBeside(SharedResource color, PlannedShare depthStencil) {
        try {
            return pool.create(depthStencil);
        } catch (RuntimeException failure) {
            pool.retire(color, NEVER_USED);
            pool.collect(NEVER_USED);
            throw failure;
        }
    }

    private int boundTexture() {
        provider.invoke("glGetIntegerv", new long[] {GL_TEXTURE_BINDING_2D, scratch.address()});
        return scratch.first();
    }
}
