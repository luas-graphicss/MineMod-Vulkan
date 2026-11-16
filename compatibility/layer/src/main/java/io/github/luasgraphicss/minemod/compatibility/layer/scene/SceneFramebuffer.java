package io.github.luasgraphicss.minemod.compatibility.layer.scene;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

public final class SceneFramebuffer {
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int BASE_LEVEL = 0;

    private SceneFramebuffer() {
    }

    public static int assemble(ProviderCalls provider, ScratchInts scratch, int colorTexture, int depthStencilTexture) {
        provider.invoke("glGenFramebuffers", new long[] {1, scratch.address()});
        int framebuffer = scratch.first();
        provider.invoke("glBindFramebuffer", new long[] {GL_FRAMEBUFFER, framebuffer});
        attach(provider, GL_COLOR_ATTACHMENT0, colorTexture);
        attach(provider, GL_DEPTH_STENCIL_ATTACHMENT, depthStencilTexture);
        long status = provider.invoke("glCheckFramebufferStatus", new long[] {GL_FRAMEBUFFER});
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            provider.invoke("glDeleteFramebuffers", new long[] {1, scratch.holding(framebuffer)});
            throw new IllegalStateException(
                    "the scene attachments do not form a complete framebuffer: 0x" + Long.toHexString(status));
        }
        return framebuffer;
    }

    private static void attach(ProviderCalls provider, int attachment, int texture) {
        provider.invoke(
                "glFramebufferTexture2D", new long[] {GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, texture, BASE_LEVEL});
    }
}
