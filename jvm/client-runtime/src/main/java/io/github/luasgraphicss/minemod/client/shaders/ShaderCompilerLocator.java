package io.github.luasgraphicss.minemod.client.shaders;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class ShaderCompilerLocator {
    public static final String PROPERTY = "minemod.shaderCompiler";

    private static final String SEARCH_PATH_VARIABLE = "PATH";
    private static final String[] EXECUTABLE_NAMES = {"glslangValidator", "glslang"};

    private ShaderCompilerLocator() {}

    public static Optional<Path> locate() {
        return locate(System.getProperty(PROPERTY), System.getenv(SEARCH_PATH_VARIABLE));
    }

    static Optional<Path> locate(String configured, String searchPath) {
        if (configured != null && !configured.isEmpty()) {
            return executableAt(configured);
        }
        if (searchPath == null) {
            return Optional.empty();
        }
        for (String directory : searchPath.split(File.pathSeparator)) {
            for (String name : EXECUTABLE_NAMES) {
                Optional<Path> found = directory.isEmpty() ? Optional.empty() : executableAt(directory, name);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> executableAt(String first, String... more) {
        try {
            Path candidate = Paths.get(first, more).toAbsolutePath();
            boolean isRunnable = Files.isRegularFile(candidate) && Files.isExecutable(candidate);
            return isRunnable ? Optional.of(candidate) : Optional.empty();
        } catch (InvalidPathException malformed) {
            return Optional.empty();
        }
    }
}
