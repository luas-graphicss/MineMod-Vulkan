package io.github.luasgraphicss.minemod.adapters.minecraft.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.luasgraphicss.minemod.client.lightmap.LightmapPublisher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class LightmapTexels {
    private LightmapTexels() {}

    public static ByteBuffer of(NativeImage lightPixels) {
        int edge = LightmapPublisher.EDGE_TEXELS;
        if (lightPixels.getWidth() != edge || lightPixels.getHeight() != edge) {
            throw new IllegalStateException("the game lightmap is " + edge + " texels wide and tall, found "
                    + lightPixels.getWidth() + "x" + lightPixels.getHeight());
        }
        ByteBuffer texels = ByteBuffer.allocate(LightmapPublisher.TEXEL_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < edge; y++) {
            for (int x = 0; x < edge; x++) {
                texels.putInt(lightPixels.getPixelRGBA(x, y));
            }
        }
        texels.flip();
        return texels;
    }
}
