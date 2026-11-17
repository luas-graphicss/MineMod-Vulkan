package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleLedger;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.provider.ProviderExtensions;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.ProviderDevice;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.ProviderMemoryImports;
import io.github.luasgraphicss.minemod.compatibility.natives.memory.JniMemory;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.state.error.ErrorLedger;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.List;
import java.util.Objects;

public final class ImportedScene implements SceneSource, AutoCloseable {
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int NO_TEXTURE = 0;

    private final JniMemory memory = new JniMemory();
    private final HandleLedger handles = new HandleLedger(HandlePlatform.LINUX_FILE_DESCRIPTOR, RendererStandIn.descriptors());
    private final StandInAttachment color;
    private final StandInAttachment depthStencil;
    private ProviderCalls provider;
    private ScratchInts scratch;
    private ProviderMemoryImports imports;

    public ImportedScene(StandInAttachment color, StandInAttachment depthStencil) {
        this.color = Objects.requireNonNull(color, "color");
        this.depthStencil = Objects.requireNonNull(depthStencil, "depthStencil");
    }

    @Override
    public SceneTextures attach(ProviderCalls calls, ScratchInts sessionScratch) {
        provider = calls;
        scratch = new ScratchInts(memory, 1);
        imports = new ProviderMemoryImports(
                calls, scratch, new ErrorLedger(() -> (int) calls.invoke("glGetError", NO_ARGUMENTS)));
        SceneTextures textures = new SceneTextures(importOf(color), importOf(depthStencil));
        calls.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, NO_TEXTURE});
        return textures;
    }

    public int importOf(StandInAttachment attachment) {
        long descriptor = attachment.exportMemory();
        handles.exported(descriptor);
        int texture;
        try {
            texture = imports.importMemory(attachment.request(), attachment.allocationSize(), descriptor);
        } catch (RuntimeException failure) {
            handles.abandoned(descriptor);
            throw failure;
        }
        handles.imported(descriptor);
        return texture;
    }

    public void release(int texture) {
        imports.release(texture);
    }

    public int liveImports() {
        return imports.liveCount();
    }

    public int descriptorsStillOwned() {
        return handles.ownedCount();
    }

    public DeviceIdentity providerIdentity() {
        return ProviderDevice.identify(provider, memory);
    }

    public List<String> providerExtensions() {
        return ProviderExtensions.list(provider, memory);
    }

    @Override
    public void close() {
        if (scratch != null) {
            scratch.close();
        }
    }
}
