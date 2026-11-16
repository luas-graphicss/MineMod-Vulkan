package io.github.luasgraphicss.minemod.client.textures;

import io.github.luasgraphicss.minemod.codec.scene.TextureRegion;

public final class TexelArea {
    public static final int TEXEL_BYTES = 4;

    private final int level;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public TexelArea(int level, int x, int y, int width, int height) {
        if (!isValid(level, x, y, width, height)) {
            throw new IllegalArgumentException("texels lie in a level, from an origin, over an area: level " + level
                    + ", " + width + "x" + height + " at " + x + "," + y);
        }
        this.level = level;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static boolean isValid(int level, int x, int y, int width, int height) {
        return level >= 0 && x >= 0 && y >= 0 && width >= 1 && height >= 1;
    }

    public long bytes() {
        return (long) width * height * TEXEL_BYTES;
    }

    int level() {
        return level;
    }

    int x() {
        return x;
    }

    int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    long rowBytes() {
        return (long) width * TEXEL_BYTES;
    }

    TexelArea rows(int first, int count) {
        return new TexelArea(level, x, y + first, width, count);
    }

    TextureRegion toRegion() {
        return new TextureRegion(level, x, y, width, height);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TexelArea)) {
            return false;
        }
        TexelArea area = (TexelArea) other;
        return level == area.level && x == area.x && y == area.y && width == area.width && height == area.height;
    }

    @Override
    public int hashCode() {
        int hash = level;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        hash = 31 * hash + width;
        return 31 * hash + height;
    }

    @Override
    public String toString() {
        return width + "x" + height + " at " + x + "," + y + " of level " + level;
    }
}
