package io.github.luasgraphicss.minemod.adapters.presentation.glfw;

import org.lwjgl.glfw.GLFW;

final class GlfwLibrary {
    private GlfwLibrary() {}

    static long createWindow(
            int width, int height, CharSequence title, long monitor, long share, boolean keepsOpenGlContext) {
        if (!keepsOpenGlContext) {
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        }
        return GLFW.glfwCreateWindow(width, height, title, monitor, share);
    }
}
