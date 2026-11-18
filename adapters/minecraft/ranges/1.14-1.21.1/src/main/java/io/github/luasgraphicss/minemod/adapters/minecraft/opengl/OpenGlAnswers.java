package io.github.luasgraphicss.minemod.adapters.minecraft.opengl;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;

public final class OpenGlAnswers {
    public static final int NO_ERROR = 0;
    public static final int FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    public static final int FIRST_ATTRIBUTE = 0;
    public static final String NO_LOG = "";
    private static final int COMPILE_STATUS = 0x8B81;
    private static final int LINK_STATUS = 0x8B82;
    private static final int MAX_TEXTURE_SIZE = 0x0D33;
    private static final int DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int VERSION = 0x1F02;
    private static final String RENDERER_NAME = "MineMod Vulkan";
    private static final String VERSION_NAME = "no OpenGL context, drawn through Vulkan";

    private OpenGlAnswers() {}

    public static int statusOf(int parameter) {
        return parameter == COMPILE_STATUS || parameter == LINK_STATUS ? TRUE : FALSE;
    }

    public static int integer(int name) {
        switch (name) {
            case MAX_TEXTURE_SIZE:
                return TextureShape.MAX_EXTENT;
            case DRAW_FRAMEBUFFER_BINDING:
                return ClientRendering.openGl().drawFramebuffer();
            default:
                return 0;
        }
    }

    public static String string(int name) {
        return name == VERSION ? VERSION_NAME : RENDERER_NAME;
    }
}
