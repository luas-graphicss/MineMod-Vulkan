package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public final class OpenDescriptors {
    private static final Path OF_THIS_PROCESS = Paths.get("/proc/self/fd");

    private OpenDescriptors() {
    }

    public static long count() throws IOException {
        try (Stream<Path> descriptors = Files.list(OF_THIS_PROCESS)) {
            return descriptors.count();
        }
    }
}
