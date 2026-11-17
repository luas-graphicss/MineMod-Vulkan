package io.github.luasgraphicss.minemod.adapters.transformers.fixtures;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class FixtureClasses {
    public static final String GAME_WINDOW = internalNameOf(GameWindow.class);
    public static final String WINDOW_HOOKS = internalNameOf(WindowHooks.class);

    public static final MethodTarget OPEN = new MethodTarget(GAME_WINDOW, "open", "()J");
    public static final MethodTarget REOPEN = new MethodTarget(GAME_WINDOW, "reopen", "()J");
    public static final MethodTarget FLIP_TWICE = new MethodTarget(GAME_WINDOW, "flipTwice", "()V");

    public static final MethodTarget CREATE_WINDOW =
            new MethodTarget(internalNameOf(WindowLibrary.class), "createWindow", "(II)J");
    public static final MethodTarget SWAP_BUFFERS =
            new MethodTarget(internalNameOf(WindowLibrary.class), "swapBuffers", "(J)V");
    public static final MethodTarget POLL_EVENTS =
            new MethodTarget(internalNameOf(WindowLibrary.class), "pollEvents", "()V");

    private static final int BUFFER_BYTES = 4096;

    private FixtureClasses() {}

    public static byte[] classFileOf(Class<?> fixture) {
        String resource = fixture.getSimpleName() + ".class";
        try (InputStream content = fixture.getResourceAsStream(resource)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_BYTES];
            for (int read = content.read(buffer); read != -1; read = content.read(buffer)) {
                bytes.write(buffer, 0, read);
            }
            return bytes.toByteArray();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static String internalNameOf(Class<?> fixture) {
        return fixture.getName().replace('.', '/');
    }
}
