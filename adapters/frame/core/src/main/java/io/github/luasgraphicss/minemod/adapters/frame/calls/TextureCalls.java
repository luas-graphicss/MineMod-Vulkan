package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import io.github.luasgraphicss.minemod.adapters.frame.textures.PackedColors;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import java.nio.IntBuffer;

public final class TextureCalls {
    private static final int TEXTURE_2D = 0x0DE1;

    private TextureCalls() {}

    public static int glGenTextures() {
        return GameFrames.current().textures().generate();
    }

    public static void glDeleteTextures(int texture) {
        GameFrames.current().textures().delete(texture);
    }

    public static void glBindTexture(int target, int texture) {
        if (target == TEXTURE_2D) {
            GameFrames.run(frame -> frame.textures().bind(texture));
        }
    }

    public static void glTexParameteri(int target, int name, int value) {
        GameFrames.current().textures().setParameter(target, name, value);
    }

    public static void glPixelStorei(int name, int value) {
        GameFrames.current().textures().store(name, value);
    }

    public static int glGetTexLevelParameteri(int target, int level, int name) {
        return GameFrames.current().textures().levelParameter(target, level, name);
    }

    public static void glTexImage2D(
            int target,
            int level,
            int internalFormat,
            int width,
            int height,
            int border,
            int format,
            int type,
            IntBuffer colors) {
        TextureObjects textures = GameFrames.current().textures();
        textures.specify(target, level, internalFormat, width, height);
        if (colors != null && TexelArea.isValid(level, 0, 0, width, height)) {
            PackedColors.write(textures, target, new TexelArea(level, 0, 0, width, height), format, type, colors);
        }
    }

    public static void glTexSubImage2D(
            int target, int level, int x, int y, int width, int height, int format, int type, IntBuffer colors) {
        if (TexelArea.isValid(level, x, y, width, height)) {
            PackedColors.write(
                    GameFrames.current().textures(), target, new TexelArea(level, x, y, width, height), format, type, colors);
        }
    }

    public static void glGetTexImage(int target, int level, int format, int type, IntBuffer into) {
        GameFrames.current().leaveOut();
    }

    public static void glCopyTexSubImage2D(
            int target, int level, int x, int y, int left, int bottom, int width, int height) {
        GameFrames.current().leaveOut();
    }
}
