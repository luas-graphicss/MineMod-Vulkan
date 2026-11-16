package io.github.luasgraphicss.minemod.bridge;

import java.nio.file.Path;

public final class ShaderServiceSetup {
    private static final int DEFAULT_WALL_TIME_MILLISECONDS = 10_000;
    private static final long DEFAULT_COMPILER_MEMORY_BYTES = 1L << 30;
    private static final long DEFAULT_CACHE_BYTES = 1L << 26;

    private final Path compiler;
    private final Path scratchDirectory;
    private final int wallTimeMilliseconds;
    private final long compilerMemoryBytes;
    private final long cacheBytes;

    private ShaderServiceSetup(
            Path compiler, Path scratchDirectory, int wallTimeMilliseconds, long compilerMemoryBytes, long cacheBytes) {
        this.compiler = compiler;
        this.scratchDirectory = scratchDirectory;
        this.wallTimeMilliseconds = wallTimeMilliseconds;
        this.compilerMemoryBytes = compilerMemoryBytes;
        this.cacheBytes = cacheBytes;
    }

    public static ShaderServiceSetup of(Path compiler, Path scratchDirectory) {
        return new ShaderServiceSetup(
                compiler.toAbsolutePath(),
                scratchDirectory.toAbsolutePath(),
                DEFAULT_WALL_TIME_MILLISECONDS,
                DEFAULT_COMPILER_MEMORY_BYTES,
                DEFAULT_CACHE_BYTES);
    }

    public ShaderServiceSetup withLimits(int wallTimeMilliseconds, long compilerMemoryBytes, long cacheBytes) {
        if (wallTimeMilliseconds < 0 || compilerMemoryBytes < 0 || cacheBytes < 0) {
            throw new IllegalArgumentException("the limits of a shader service cannot be negative");
        }
        return new ShaderServiceSetup(
                compiler, scratchDirectory, wallTimeMilliseconds, compilerMemoryBytes, cacheBytes);
    }

    public Path compiler() {
        return compiler;
    }

    public Path scratchDirectory() {
        return scratchDirectory;
    }

    public int wallTimeMilliseconds() {
        return wallTimeMilliseconds;
    }

    public long compilerMemoryBytes() {
        return compilerMemoryBytes;
    }

    public long cacheBytes() {
        return cacheBytes;
    }

    @Override
    public String toString() {
        return "compiler " + compiler + " in " + scratchDirectory + " (" + wallTimeMilliseconds + " ms, "
                + compilerMemoryBytes + " compiler bytes, " + cacheBytes + " cache bytes)";
    }
}
