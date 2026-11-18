package io.github.luasgraphicss.minemod.adapters.minecraft.overlay;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.luasgraphicss.minemod.client.overlay.OverlayPublisher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class OverlayTexels {
    private OverlayTexels() {}

    public static ByteBuffer of(NativeImage overlayPixels) {
        int edge = OverlayPublisher.EDGE_TEXELS;
        if (overlayPixels.getWidth() != edge || overlayPixels.getHeight() != edge) {
            throw new IllegalStateException("the game overlay is " + edge + " texels wide and tall, found "
                    + overlayPixels.getWidth() + "x" + overlayPixels.getHeight());
        }
        ByteBuffer texels = ByteBuffer.allocate(OverlayPublisher.TEXEL_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < edge; y++) {
            for (int x = 0; x < edge; x++) {
                texels.putInt(overlayPixels.getPixelRGBA(x, y));
            }
        }
        texels.flip();
        return texels;
    }
}
