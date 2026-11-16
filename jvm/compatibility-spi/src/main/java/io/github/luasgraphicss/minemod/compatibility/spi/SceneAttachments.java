package io.github.luasgraphicss.minemod.compatibility.spi;

public final class SceneAttachments {
    private final int width;
    private final int height;
    private final int samples;

    public SceneAttachments(int width, int height, int samples) {
        if (width < 1 || height < 1 || samples < 1) {
            throw new IllegalArgumentException(
                    "scene attachments have a positive size and sample count: " + width + "x" + height + ", " + samples);
        }
        this.width = width;
        this.height = height;
        this.samples = samples;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int samples() {
        return samples;
    }
}
