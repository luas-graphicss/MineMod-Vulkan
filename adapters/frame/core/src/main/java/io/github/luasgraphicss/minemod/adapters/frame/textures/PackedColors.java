package io.github.luasgraphicss.minemod.adapters.frame.textures;

import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class PackedColors {
    private static final int BLUE_GREEN_RED_ALPHA = 0x80E1;
    private static final int PACKED_FROM_THE_LAST_BYTE = 0x8367;
    private static final int RGBA = 0x1908;
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int ALPHA_SHIFT = 24;

    private PackedColors() {}

    public static void write(TextureObjects textures, int target, TexelArea area, int format, int type, IntBuffer colors) {
        long texels = (long) area.width() * area.height();
        boolean isPacked = format == BLUE_GREEN_RED_ALPHA && type == PACKED_FROM_THE_LAST_BYTE;
        if (!isPacked || colors.remaining() < texels || area.bytes() > Integer.MAX_VALUE) {
            return;
        }
        ByteBuffer rgba = ByteBuffer.allocateDirect((int) area.bytes());
        int first = colors.position();
        for (int texel = 0; texel < texels; texel++) {
            int color = colors.get(first + texel);
            rgba.put((byte) (color >> RED_SHIFT))
                    .put((byte) (color >> GREEN_SHIFT))
                    .put((byte) color)
                    .put((byte) (color >>> ALPHA_SHIFT));
        }
        rgba.flip();
        textures.write(target, area, RGBA, UNSIGNED_BYTE, rgba);
    }
}
