package io.github.luasgraphicss.minemod.verification.frame.fixtures;

import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.CommittedManifests;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.FixtureClient;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class FrameFixtures {
    public static final String RELEASE = "1.13";
    public static final int LWJGL_GENERATION = 3;
    public static final String GAME_CLASS = "a";
    public static final String MATRIX_STAND_IN = "io/github/luasgraphicss/minemod/adapters/frame/calls/MatrixCalls";
    public static final LibraryCall MATRIX_MODE = openGl11("glMatrixMode", "(I)V");
    public static final LibraryCall LOAD_IDENTITY = openGl11("glLoadIdentity", "()V");
    public static final LibraryCall NEVER_ANSWERED = openGl11("glNeverAnswered", "()V");
    public static final LibraryCall USE_PROGRAM =
            new LibraryCall("GL20.glUseProgram(I)V", "org/lwjgl/opengl/GL20", "glUseProgram", "(I)V");

    private static final String OPENGL_11 = "org/lwjgl/opengl/GL11";

    private FrameFixtures() {}

    public static ReleaseTarget release() throws IOException {
        return CommittedManifests.targets().releases().stream()
                .filter(release -> release.id().equals(RELEASE))
                .findFirst()
                .orElseThrow();
    }

    public static ClientArchive client(Path jar) throws IOException {
        return clientCalling(
                jar, Map.of("m", List.of(MATRIX_MODE, LOAD_IDENTITY, LOAD_IDENTITY), "n", List.of(MATRIX_MODE)));
    }

    public static ClientArchive clientCalling(Path jar, Map<String, List<LibraryCall>> callsByMethod)
            throws IOException {
        byte[] gameClass = FixtureClient.classCalling(GAME_CLASS, callsByMethod);
        return ClientArchive.open(FixtureClient.jarOf(jar, Map.of(GAME_CLASS, gameClass)));
    }

    private static LibraryCall openGl11(String name, String descriptor) {
        return new LibraryCall("GL11." + name + descriptor, OPENGL_11, name, descriptor);
    }
}
