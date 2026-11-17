package io.github.luasgraphicss.minemod.adapters.presentation.glfw;

import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public final class GlfwPresentationHooks {
    public static final String INTERNAL_NAME =
            "io/github/luasgraphicss/minemod/adapters/presentation/glfw/GlfwPresentationHooks";

    private GlfwPresentationHooks() {}

    public static long glfwCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
        PresentationOwner owner = PresentationOwners.current();
        return owner.openWindow(
                WindowRequest.ofSize(width, height, title),
                () -> GlfwLibrary.createWindow(width, height, title, monitor, share, owner.keepsOpenGlContext()));
    }

    public static void glfwMakeContextCurrent(long window) {
        PresentationOwners.current().attachContext(window, () -> GLFW.glfwMakeContextCurrent(window));
    }

    public static GLCapabilities createCapabilities() {
        return (GLCapabilities) PresentationOwners.current().resolveCapabilities(GL::createCapabilities);
    }

    public static void glfwSwapBuffers(long window) {
        PresentationOwners.current().presentFrame(window, () -> GLFW.glfwSwapBuffers(window));
    }

    public static void glfwSwapInterval(int interval) {
        PresentationOwners.current().pace(interval, () -> GLFW.glfwSwapInterval(interval));
    }

    public static void glfwDestroyWindow(long window) {
        PresentationOwners.current().closeWindow(window, () -> GLFW.glfwDestroyWindow(window));
    }
}
