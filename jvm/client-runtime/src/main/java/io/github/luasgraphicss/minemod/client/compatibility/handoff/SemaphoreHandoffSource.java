package io.github.luasgraphicss.minemod.client.compatibility.handoff;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.bridge.sharing.AttachmentDescription;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSemaphores;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.client.compatibility.sharing.ImportedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.handoff.SharedAttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.ProviderHandoff;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.RendererHandoff;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.SplitSharingBackend;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class SemaphoreHandoffSource implements HandoffSource {
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int GL_NO_ERROR = 0;
    private static final int VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL = 2;
    private static final int VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL = 3;
    private static final int ONE = 1;

    private final SceneSharing sharing;
    private final RendererHandoff renderer;
    private final ProviderCalls provider;
    private final NativeMemory memory;
    private final ScratchInts scratch;
    private final int rendererDone;
    private final int providerDone;
    private ProviderHandoff current;

    SemaphoreHandoffSource(
            SceneSharing sharing, RendererHandoff renderer, ProviderCalls provider, NativeMemory memory) {
        this.sharing = sharing;
        this.renderer = renderer;
        this.provider = provider;
        this.memory = memory;
        this.scratch = new ScratchInts(memory, 1);
        HandoffSemaphores exported = sharing.exportSemaphores();
        try {
            this.rendererDone = importedOrClosed(exported.rendererDone());
        } catch (RuntimeException failure) {
            sharing.closeExported(exported.providerDone());
            scratch.close();
            throw failure;
        }
        try {
            this.providerDone = importedOrClosed(exported.providerDone());
        } catch (RuntimeException failure) {
            delete(rendererDone);
            scratch.close();
            throw failure;
        }
    }

    @Override
    public AttachmentHandoff over(ImportedAttachments imported) {
        if (current != null) {
            current.close();
        }
        SceneTextures textures = imported.textures();
        current = new ProviderHandoff(
                provider,
                memory,
                new int[] {textures.color(), textures.depthStencil()},
                new int[] {
                    layoutOf(imported.shared().attachment(SharedAttachment.COLOR)),
                    layoutOf(imported.shared().attachment(SharedAttachment.DEPTH_STENCIL))
                },
                rendererDone,
                providerDone);
        return new SharedAttachmentHandoff(new SplitSharingBackend(renderer, current));
    }

    @Override
    public void close() {
        if (current != null) {
            current.close();
        }
        delete(rendererDone);
        delete(providerDone);
        scratch.close();
    }

    private int importedOrClosed(long descriptor) {
        int semaphore = ProviderHandoff.importSemaphore(provider, scratch, descriptor);
        int error = (int) provider.invoke("glGetError", NO_ARGUMENTS);
        if (error == GL_NO_ERROR) {
            return semaphore;
        }
        delete(semaphore);
        sharing.closeExported(descriptor);
        throw new IllegalStateException(
                "the provider refused to import a handoff semaphore with error 0x" + Integer.toHexString(error));
    }

    private void delete(int semaphore) {
        provider.invoke("glDeleteSemaphoresEXT", new long[] {ONE, scratch.holding(semaphore)});
    }

    private static int layoutOf(AttachmentDescription attachment) {
        switch (attachment.vulkanLayout()) {
            case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL:
                return ProviderHandoff.LAYOUT_COLOR_ATTACHMENT;
            case VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL:
                return ProviderHandoff.LAYOUT_DEPTH_STENCIL_ATTACHMENT;
            default:
                throw new IllegalArgumentException(
                        "no OpenGL layout is paired with VkImageLayout " + attachment.vulkanLayout());
        }
    }
}
