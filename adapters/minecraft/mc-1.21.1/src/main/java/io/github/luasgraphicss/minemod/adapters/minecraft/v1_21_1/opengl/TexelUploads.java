package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.opengl;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import org.lwjgl.system.MemoryUtil;

public final class TexelUploads {
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int TEXTURE_2D = 0x0DE1;
    private static final int UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int UNPACK_SKIP_ROWS = 0x0CF3;
    private static final int UNPACK_SKIP_PIXELS = 0x0CF4;

    private TexelUploads() {}

    public static void write(int target, TexelArea area, int format, int type, long texels) {
        TextureObjects textures = ClientRendering.openGl().textures();
        long sourceBytes = textures.sourceBytes(format, area.width(), area.height());
        if (texels == MemoryUtil.NULL || sourceBytes < 1 || sourceBytes > Integer.MAX_VALUE) {
            return;
        }
        textures.write(target, area, format, type, MemoryUtil.memByteBuffer(texels, (int) sourceBytes));
    }

    public static void writeUnpadded(TexelArea area, int format, long texels) {
        TextureObjects textures = ClientRendering.openGl().textures();
        textures.store(UNPACK_ROW_LENGTH, area.width());
        textures.store(UNPACK_SKIP_PIXELS, 0);
        textures.store(UNPACK_SKIP_ROWS, 0);
        write(TEXTURE_2D, area, format, UNSIGNED_BYTE, texels);
    }
}
