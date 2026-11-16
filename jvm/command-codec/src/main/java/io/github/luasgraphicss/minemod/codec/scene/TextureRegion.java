package io.github.luasgraphicss.minemod.codec.scene;

import java.nio.ByteBuffer;

public final class TextureRegion {
    static final int TEXEL_BYTES = 4;

    private final int level;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public TextureRegion(int level, int x, int y, int width, int height) {
        if (level < 0 || x < 0 || y < 0 || width < 1 || height < 1) {
            throw new IllegalArgumentException(
                    "a texture region needs a level, an origin and an area: level " + level + ", " + width + "x"
                            + height + " at " + x + "," + y);
        }
        this.level = level;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    long texelBytes() {
        return (long) width * height * TEXEL_BYTES;
    }

    void put(ByteBuffer payload) {
        payload.putInt(level).putInt(x).putInt(y).putInt(width).putInt(height);
    }
}
