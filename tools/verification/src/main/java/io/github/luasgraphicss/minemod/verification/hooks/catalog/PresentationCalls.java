package io.github.luasgraphicss.minemod.verification.hooks.catalog;

import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import java.util.List;

public final class PresentationCalls {
    private static final String GLFW = "org/lwjgl/glfw/GLFW";
    private static final String GL = "org/lwjgl/opengl/GL";
    private static final String DISPLAY = "org/lwjgl/opengl/Display";

    private static final List<LibraryCall> GLFW_CALLS = List.of(
            new LibraryCall("window-create", GLFW, "glfwCreateWindow", "(IILjava/lang/CharSequence;JJ)J"),
            new LibraryCall("context-current", GLFW, "glfwMakeContextCurrent", "(J)V"),
            new LibraryCall("capabilities", GL, "createCapabilities", "()Lorg/lwjgl/opengl/GLCapabilities;"),
            new LibraryCall("swap-buffers", GLFW, "glfwSwapBuffers", "(J)V"),
            new LibraryCall("swap-interval", GLFW, "glfwSwapInterval", "(I)V"),
            new LibraryCall("window-destroy", GLFW, "glfwDestroyWindow", "(J)V"));

    private static final List<LibraryCall> DISPLAY_CALLS = List.of(
            new LibraryCall("display-create", DISPLAY, "create", "(Lorg/lwjgl/opengl/PixelFormat;)V"),
            new LibraryCall("display-create-default", DISPLAY, "create", "()V"),
            new LibraryCall("display-update", DISPLAY, "update", "()V"),
            new LibraryCall("display-vsync", DISPLAY, "setVSyncEnabled", "(Z)V"),
            new LibraryCall("display-destroy", DISPLAY, "destroy", "()V"));

    private PresentationCalls() {}

    public static List<LibraryCall> of(Windowing windowing) {
        return switch (windowing) {
            case GLFW -> GLFW_CALLS;
            case LWJGL2_DISPLAY -> DISPLAY_CALLS;
        };
    }
}
