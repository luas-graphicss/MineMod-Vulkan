package io.github.luasgraphicss.minemod.adapters.minecraft.opengl;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class PackedColorTexels {
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int RGBA = 0x1908;
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int UNPACK_SKIP_ROWS = 0x0CF3;
    private static final int UNPACK_SKIP_PIXELS = 0x0CF4;
    private static final int WRAP_S = 0x2802;
    private static final int WRAP_T = 0x2803;
    private static final int MAG_FILTER = 0x2800;
    private static final int MIN_FILTER = 0x2801;
    private static final int REPEAT = 0x2901;
    private static final int NEAREST = 0x2600;
    private static final int LINEAR = 0x2601;
    private static final int FIRST_LEVEL = 0;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int ALPHA_SHIFT = 24;

    private PackedColorTexels() {}

    public static void write(IntBuffer colors, int width, int height) {
        TextureObjects textures = ClientRendering.openGl().textures();
        textures.specify(TEXTURE_2D, FIRST_LEVEL, RGBA, width, height);
        textures.setParameter(TEXTURE_2D, WRAP_S, REPEAT);
        textures.setParameter(TEXTURE_2D, WRAP_T, REPEAT);
        textures.setParameter(TEXTURE_2D, MAG_FILTER, NEAREST);
        textures.setParameter(TEXTURE_2D, MIN_FILTER, LINEAR);
        long texels = (long) width * height;
        if (!TexelArea.isValid(FIRST_LEVEL, 0, 0, width, height) || colors.remaining() < texels) {
            return;
        }
        textures.store(UNPACK_ROW_LENGTH, 0);
        textures.store(UNPACK_SKIP_ROWS, 0);
        textures.store(UNPACK_SKIP_PIXELS, 0);
        textures.write(
                TEXTURE_2D,
                new TexelArea(FIRST_LEVEL, 0, 0, width, height),
                RGBA,
                UNSIGNED_BYTE,
                rgbaOf(colors, (int) texels));
    }

    private static ByteBuffer rgbaOf(IntBuffer colors, int texels) {
        ByteBuffer rgba = ByteBuffer.allocate(texels * TexelArea.TEXEL_BYTES);
        for (int texel = 0; texel < texels; texel++) {
            int color = colors.get(colors.position() + texel);
            rgba.put((byte) (color >> RED_SHIFT))
                    .put((byte) (color >> GREEN_SHIFT))
                    .put((byte) color)
                    .put((byte) (color >>> ALPHA_SHIFT));
        }
        rgba.flip();
        return rgba;
    }
}
