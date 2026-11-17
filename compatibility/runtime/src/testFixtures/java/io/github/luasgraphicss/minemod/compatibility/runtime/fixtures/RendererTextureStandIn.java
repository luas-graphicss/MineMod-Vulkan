package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class RendererTextureStandIn implements SceneSource {
    public static final int SIDE = 4;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_WIDTH = 0x1000;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int ONE_LEVEL = 1;
    private static final int BASE_LEVEL = 0;
    private static final int CHANNELS = 4;

    private final SceneSource scene;
    private final int[] color;
    private ProviderCalls provider;
    private int backing;

    public RendererTextureStandIn(SceneSource scene, int[] color) {
        this.scene = Objects.requireNonNull(scene, "scene");
        this.color = color.clone();
    }

    @Override
    public SceneTextures attach(ProviderCalls calls, ScratchInts scratch) {
        provider = calls;
        calls.invoke("glGenTextures", new long[] {1, scratch.address()});
        backing = scratch.first();
        calls.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, backing});
        calls.invoke("glTexStorage2D", new long[] {GL_TEXTURE_2D, ONE_LEVEL, GL_RGBA8, SIDE, SIDE});
        try (NativeBlock texels = new NativeBlock(SIDE * SIDE * CHANNELS)) {
            texels.setBytes(filledWithColor());
            calls.invoke("glTexSubImage2D", new long[] {
                GL_TEXTURE_2D, BASE_LEVEL, 0, 0, SIDE, SIDE, GL_RGBA, GL_UNSIGNED_BYTE, texels.address()
            });
        }
        return scene.attach(calls, scratch);
    }

    public int backing() {
        return backing;
    }

    public boolean isTextureInProvider(int providerName) {
        return provider.invoke("glIsTexture", new long[] {providerName}) != 0;
    }

    public int widthInProvider() {
        try (NativeBlock answer = new NativeBlock(Integer.BYTES)) {
            provider.invoke("glGetIntegerv", new long[] {GL_TEXTURE_BINDING_2D, answer.address()});
            int boundByTheLayer = answer.intAt(0);
            provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, backing});
            provider.invoke(
                    "glGetTexLevelParameteriv", new long[] {GL_TEXTURE_2D, BASE_LEVEL, GL_TEXTURE_WIDTH, answer.address()});
            int width = answer.intAt(0);
            provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, boundByTheLayer});
            return width;
        }
    }

    private byte[] filledWithColor() {
        byte[] texels = new byte[SIDE * SIDE * CHANNELS];
        for (int index = 0; index < texels.length; index++) {
            texels[index] = (byte) color[index % CHANNELS];
        }
        return texels;
    }
}
