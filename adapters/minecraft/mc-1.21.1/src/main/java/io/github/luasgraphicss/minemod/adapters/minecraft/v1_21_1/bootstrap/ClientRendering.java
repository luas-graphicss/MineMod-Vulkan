package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.window.GlfwWindowSurface;
import io.github.luasgraphicss.minemod.client.opengl.OpenGlObjects;
import io.github.luasgraphicss.minemod.client.session.ClientRenderer;
import io.github.luasgraphicss.minemod.client.session.DeviceValidation;
import io.github.luasgraphicss.minemod.client.session.SceneAttachments;
import java.nio.file.Path;
import java.util.Optional;

public final class ClientRendering {
    private static final String VALIDATION_PROPERTY = "minemod.validation";

    private static volatile Path nativeLibrary;
    private static volatile ClientRenderer renderer;
    private static volatile OpenGlObjects openGl;

    private ClientRendering() {}

    public static void prepare(Path extractedLibrary) {
        nativeLibrary = extractedLibrary;
    }

    public static void attach(long window) {
        Path library = nativeLibrary;
        if (library == null) {
            throw new IllegalStateException("the loader adapter prepares the native library before the window opens");
        }
        DeviceValidation validation =
                Boolean.getBoolean(VALIDATION_PROPERTY) ? DeviceValidation.ENABLED : DeviceValidation.DISABLED;
        SceneAttachments attachments = ModRendering.isPrepared() ? SceneAttachments.SHARED : SceneAttachments.PRIVATE;
        ClientRenderer started = ClientRenderer.start(library, new GlfwWindowSurface(window), validation, attachments);
        try {
            ModRendering.attach(started);
        } catch (RuntimeException failure) {
            started.close();
            throw failure;
        }
        openGl = started.openGl();
        renderer = started;
    }

    public static boolean replacesOpenGl() {
        return openGl != null;
    }

    public static OpenGlObjects openGl() {
        OpenGlObjects objects = openGl;
        if (objects == null) {
            throw new IllegalStateException("the native renderer replaces OpenGL once the game window is open");
        }
        return objects;
    }

    public static ClientRenderer renderer() {
        return running().orElseThrow(
                () -> new IllegalStateException("the native renderer runs while the game window is open"));
    }

    public static Optional<ClientRenderer> running() {
        return Optional.ofNullable(renderer);
    }

    public static void detach() {
        ClientRenderer stopped = renderer;
        renderer = null;
        if (stopped != null) {
            ModRendering.detach();
            stopped.close();
        }
    }
}
