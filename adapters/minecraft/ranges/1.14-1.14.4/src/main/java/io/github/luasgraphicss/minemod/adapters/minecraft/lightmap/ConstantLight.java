package io.github.luasgraphicss.minemod.adapters.minecraft.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.luasgraphicss.minemod.client.lightmap.LightmapPublisher;

public final class ConstantLight {
    private static final float COORDINATES_PER_TEXEL = 16.0f;
    private static final float BRIGHTEST_CHANNEL = 255.0f;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int GREEN_SHIFT = 8;
    private static final int BLUE_SHIFT = 16;
    private static final int LAST_TEXEL = LightmapPublisher.EDGE_TEXELS - 1;

    private static NativeImage texels;
    private static float blockLight;
    private static float skyLight;

    private ConstantLight() {}

    public static void lookUpIn(NativeImage lightPixels) {
        texels = lightPixels;
    }

    public static void coordinate(float block, float sky) {
        blockLight = block;
        skyLight = sky;
    }

    public static float[] color() {
        if (texels == null) {
            return new float[] {1.0f, 1.0f, 1.0f};
        }
        int texel = texels.getPixelRGBA(texelAt(blockLight), texelAt(skyLight));
        return new float[] {
            (texel & CHANNEL_MASK) / BRIGHTEST_CHANNEL,
            (texel >> GREEN_SHIFT & CHANNEL_MASK) / BRIGHTEST_CHANNEL,
            (texel >> BLUE_SHIFT & CHANNEL_MASK) / BRIGHTEST_CHANNEL
        };
    }

    private static int texelAt(float coordinate) {
        return Math.max(0, Math.min(LAST_TEXEL, (int) (coordinate / COORDINATES_PER_TEXEL)));
    }
}
