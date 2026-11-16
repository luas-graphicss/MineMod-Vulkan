package io.github.luasgraphicss.minemod.client.frame;

import java.nio.ByteBuffer;

public final class FramePixels {
    public static final int PIXEL_BYTES = 4;

    private final int width;
    private final int height;
    private final ByteBuffer rgba;

    public FramePixels(int width, int height, ByteBuffer rgba) {
        if (width < 1 || height < 1 || rgba.remaining() != (long) width * height * PIXEL_BYTES) {
            throw new IllegalArgumentException("a frame of " + width + "x" + height + " holds " + PIXEL_BYTES
                    + " bytes for each pixel, found " + rgba.remaining());
        }
        this.width = width;
        this.height = height;
        this.rgba = rgba;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public ByteBuffer rgba() {
        return rgba.duplicate();
    }
}
