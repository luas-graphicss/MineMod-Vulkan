package io.github.luasgraphicss.minemod.adapters.frame.glfw.calls;

import io.github.luasgraphicss.minemod.adapters.frame.answers.FrameDownload;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrame;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import io.github.luasgraphicss.minemod.client.frame.FramePixels;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import org.lwjgl.system.MemoryUtil;

public final class AddressedTexelCalls {
    private static final int RGBA = 0x1908;
    private static final int LUMINANCE_ALPHA = 0x190A;
    private static final int RED_GREEN = 0x8227;
    private static final int UNSIGNED_BYTE = 0x1401;

    private AddressedTexelCalls() {}

    public static void glTexSubImage2D(
            int target, int level, int x, int y, int width, int height, int format, int type, long texels) {
        TextureObjects textures = GameFrames.current().textures();
        int readFormat = format == LUMINANCE_ALPHA ? RED_GREEN : format;
        long sourceBytes = textures.sourceBytes(readFormat, width, height);
        boolean isReadable = texels != MemoryUtil.NULL && sourceBytes >= 1 && sourceBytes <= Integer.MAX_VALUE;
        if (isReadable && TexelArea.isValid(level, x, y, width, height)) {
            textures.write(
                    target,
                    new TexelArea(level, x, y, width, height),
                    readFormat,
                    type,
                    MemoryUtil.memByteBuffer(texels, (int) sourceBytes));
        }
    }

    public static void glGetTexImage(int target, int level, int format, int type, long into) {
        GameFrames.current().leaveOut();
    }

    public static void glReadPixels(int left, int bottom, int width, int height, int format, int type, long into) {
        GameFrame frame = GameFrames.current();
        long bytes = (long) width * height * FramePixels.PIXEL_BYTES;
        boolean isWritable = format == RGBA && type == UNSIGNED_BYTE && into != MemoryUtil.NULL;
        if (!isWritable || bytes < 1 || bytes > Integer.MAX_VALUE) {
            frame.leaveOut();
            return;
        }
        FrameDownload.asRgba(frame.scene(), width, height, MemoryUtil.memByteBuffer(into, (int) bytes));
    }
}
