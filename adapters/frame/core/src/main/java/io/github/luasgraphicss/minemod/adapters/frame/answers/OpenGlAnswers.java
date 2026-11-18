package io.github.luasgraphicss.minemod.adapters.frame.answers;

import io.github.luasgraphicss.minemod.client.textures.TextureShape;

public final class OpenGlAnswers {
    public static final int NO_ERROR = 0;
    private static final int MAX_TEXTURE_SIZE = 0x0D33;
    private static final int VERSION = 0x1F02;
    private static final int EXTENSIONS = 0x1F03;
    private static final String RENDERER_NAME = "MineMod Vulkan";
    private static final String VERSION_NAME = "2.0 without an OpenGL context, drawn through Vulkan";
    private static final String NO_EXTENSIONS = "";

    private OpenGlAnswers() {}

    public static int integer(int name) {
        return name == MAX_TEXTURE_SIZE ? TextureShape.MAX_EXTENT : 0;
    }

    public static String string(int name) {
        switch (name) {
            case VERSION:
                return VERSION_NAME;
            case EXTENSIONS:
                return NO_EXTENSIONS;
            default:
                return RENDERER_NAME;
        }
    }
}
