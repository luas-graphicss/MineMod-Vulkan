package io.github.luasgraphicss.minemod.bridge;

public final class SceneSessionSetup {
    private static final long NO_WINDOW_SURFACE = 0;
    private static final long DEFAULT_ARENA_BYTES = 32L * 1024 * 1024;
    private static final int DEFAULT_MAX_TEXTURES = 4096;
    private static final int DEFAULT_MAX_SECTIONS = 65536;

    private final int width;
    private final int height;
    private final long windowSurface;
    private final long uploadBytesPerFrame;
    private final long geometryBytesPerFrame;
    private final int maxTextures;
    private final int maxSections;

    private SceneSessionSetup(
            int width,
            int height,
            long windowSurface,
            long uploadBytesPerFrame,
            long geometryBytesPerFrame,
            int maxTextures,
            int maxSections) {
        this.width = width;
        this.height = height;
        this.windowSurface = windowSurface;
        this.uploadBytesPerFrame = uploadBytesPerFrame;
        this.geometryBytesPerFrame = geometryBytesPerFrame;
        this.maxTextures = maxTextures;
        this.maxSections = maxSections;
    }

    public static SceneSessionSetup offscreen(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("a frame needs an area: " + width + "x" + height);
        }
        return new SceneSessionSetup(
                width,
                height,
                NO_WINDOW_SURFACE,
                DEFAULT_ARENA_BYTES,
                DEFAULT_ARENA_BYTES,
                DEFAULT_MAX_TEXTURES,
                DEFAULT_MAX_SECTIONS);
    }

    public SceneSessionSetup presentingOn(long surfaceOfTheWindow) {
        if (surfaceOfTheWindow == NO_WINDOW_SURFACE) {
            throw new IllegalArgumentException("the window system created no surface");
        }
        return new SceneSessionSetup(
                width, height, surfaceOfTheWindow, uploadBytesPerFrame, geometryBytesPerFrame, maxTextures, maxSections);
    }

    public SceneSessionSetup withArenas(long uploadBytes, long geometryBytes) {
        if (uploadBytes < 1 || geometryBytes < 1) {
            throw new IllegalArgumentException(
                    "the arenas of a frame need room: " + uploadBytes + " and " + geometryBytes + " bytes");
        }
        return new SceneSessionSetup(width, height, windowSurface, uploadBytes, geometryBytes, maxTextures, maxSections);
    }

    public SceneSessionSetup withLimits(int textures, int sections) {
        if (textures < 1 || sections < 1) {
            throw new IllegalArgumentException(
                    "a session holds at least one texture and one section: " + textures + " and " + sections);
        }
        return new SceneSessionSetup(
                width, height, windowSurface, uploadBytesPerFrame, geometryBytesPerFrame, textures, sections);
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    long windowSurface() {
        return windowSurface;
    }

    long uploadBytesPerFrame() {
        return uploadBytesPerFrame;
    }

    long geometryBytesPerFrame() {
        return geometryBytesPerFrame;
    }

    int maxTextures() {
        return maxTextures;
    }

    int maxSections() {
        return maxSections;
    }

    @Override
    public String toString() {
        return width + "x" + height + (windowSurface == NO_WINDOW_SURFACE ? " offscreen" : " on a window") + ", "
                + uploadBytesPerFrame + " upload and " + geometryBytesPerFrame + " geometry bytes per frame, "
                + maxTextures + " textures, " + maxSections + " sections";
    }
}
