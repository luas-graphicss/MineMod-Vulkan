package io.github.luasgraphicss.minemod.client.window;

public final class FramebufferSize {
    private final int width;
    private final int height;

    public FramebufferSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("a framebuffer edge cannot be negative: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean hasArea() {
        return width > 0 && height > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof FramebufferSize)) {
            return false;
        }
        FramebufferSize size = (FramebufferSize) other;
        return width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
