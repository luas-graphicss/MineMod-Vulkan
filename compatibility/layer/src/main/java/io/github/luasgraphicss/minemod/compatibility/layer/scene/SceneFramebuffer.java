package io.github.luasgraphicss.minemod.compatibility.layer.scene;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

public final class SceneFramebuffer {
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int GL_NO_ERROR = 0;
    private static final int GL_TRUE = 1;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_FRAMEBUFFER_FLIP_Y_MESA = 0x8BBB;
    private static final int BASE_LEVEL = 0;

    private SceneFramebuffer() {
    }

    public static int assemble(ProviderCalls provider, ScratchInts scratch, int colorTexture, int depthStencilTexture) {
        provider.invoke("glGenFramebuffers", new long[] {1, scratch.address()});
        int framebuffer = scratch.first();
        provider.invoke("glBindFramebuffer", new long[] {GL_FRAMEBUFFER, framebuffer});
        provider.invoke("glFramebufferParameteri", new long[] {GL_FRAMEBUFFER, GL_FRAMEBUFFER_FLIP_Y_MESA, GL_TRUE});
        long orientationError = provider.invoke("glGetError", NO_ARGUMENTS);
        if (orientationError != GL_NO_ERROR) {
            throw discarded(
                    provider,
                    scratch,
                    framebuffer,
                    "the provider cannot draw the scene with its first row at the top: 0x"
                            + Long.toHexString(orientationError));
        }
        long status = attachBoth(provider, colorTexture, depthStencilTexture);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw discarded(
                    provider,
                    scratch,
                    framebuffer,
                    "the scene attachments do not form a complete framebuffer: 0x" + Long.toHexString(status));
        }
        return framebuffer;
    }

    public static void reattach(
            ProviderCalls provider, ScratchInts scratch, int framebuffer, int colorTexture, int depthStencilTexture) {
        int drawBinding = bindingOf(provider, scratch, GL_DRAW_FRAMEBUFFER_BINDING);
        int readBinding = bindingOf(provider, scratch, GL_READ_FRAMEBUFFER_BINDING);
        provider.invoke("glBindFramebuffer", new long[] {GL_FRAMEBUFFER, framebuffer});
        long status = attachBoth(provider, colorTexture, depthStencilTexture);
        provider.invoke("glBindFramebuffer", new long[] {GL_DRAW_FRAMEBUFFER, drawBinding});
        provider.invoke("glBindFramebuffer", new long[] {GL_READ_FRAMEBUFFER, readBinding});
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException(
                    "the replaced scene attachments do not form a complete framebuffer: 0x" + Long.toHexString(status));
        }
    }

    private static IllegalStateException discarded(
            ProviderCalls provider, ScratchInts scratch, int framebuffer, String defect) {
        provider.invoke("glDeleteFramebuffers", new long[] {1, scratch.holding(framebuffer)});
        return new IllegalStateException(defect);
    }

    private static long attachBoth(ProviderCalls provider, int colorTexture, int depthStencilTexture) {
        attach(provider, GL_COLOR_ATTACHMENT0, colorTexture);
        attach(provider, GL_DEPTH_STENCIL_ATTACHMENT, depthStencilTexture);
        return provider.invoke("glCheckFramebufferStatus", new long[] {GL_FRAMEBUFFER});
    }

    private static int bindingOf(ProviderCalls provider, ScratchInts scratch, int binding) {
        provider.invoke("glGetIntegerv", new long[] {binding, scratch.address()});
        return scratch.first();
    }

    private static void attach(ProviderCalls provider, int attachment, int texture) {
        provider.invoke(
                "glFramebufferTexture2D", new long[] {GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, texture, BASE_LEVEL});
    }
}
