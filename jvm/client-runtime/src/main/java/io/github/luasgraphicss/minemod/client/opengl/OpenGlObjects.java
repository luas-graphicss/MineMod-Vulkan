package io.github.luasgraphicss.minemod.client.opengl;

import io.github.luasgraphicss.minemod.client.opengl.buffers.BufferStorage;
import io.github.luasgraphicss.minemod.client.opengl.programs.ShaderPrograms;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexturePublisher;

public final class OpenGlObjects {
    private static final int READ_FRAMEBUFFER = 0x8CA8;

    private final TextureObjects textures;
    private final ShaderPrograms programs = new ShaderPrograms();
    private final BufferStorage buffers = new BufferStorage();
    private int lastFramebufferName;
    private int drawFramebuffer;

    public OpenGlObjects(TexturePublisher publishedTextures) {
        this.textures = new TextureObjects(publishedTextures);
    }

    public TextureObjects textures() {
        return textures;
    }

    public ShaderPrograms programs() {
        return programs;
    }

    public BufferStorage buffers() {
        return buffers;
    }

    public int generateFramebuffer() {
        return ++lastFramebufferName;
    }

    public void bindFramebuffer(int target, int framebuffer) {
        if (target != READ_FRAMEBUFFER) {
            drawFramebuffer = framebuffer;
        }
    }

    public int drawFramebuffer() {
        return drawFramebuffer;
    }
}
