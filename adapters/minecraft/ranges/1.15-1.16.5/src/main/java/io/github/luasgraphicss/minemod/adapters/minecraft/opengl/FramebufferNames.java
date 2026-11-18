package io.github.luasgraphicss.minemod.adapters.minecraft.opengl;

import com.mojang.blaze3d.platform.GlConst;

public final class FramebufferNames {
    public static final String SUPPORT = "the native renderer";
    public static final int NO_DEPTH_TEXTURE = 0;

    private FramebufferNames() {}

    public static void assign() {
        GlConst.GL_FRAMEBUFFER = 0x8D40;
        GlConst.GL_RENDERBUFFER = 0x8D41;
        GlConst.GL_COLOR_ATTACHMENT0 = 0x8CE0;
        GlConst.GL_DEPTH_ATTACHMENT = 0x8D00;
        GlConst.GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
        GlConst.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
        GlConst.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
        GlConst.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 0x8CDB;
        GlConst.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 0x8CDC;
    }
}
