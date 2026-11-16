package io.github.luasgraphicss.minemod.client.textures;

public final class TextureShape {
    public static final int MAX_EXTENT = 16384;

    private final int width;
    private final int height;
    private final int levels;

    public TextureShape(int width, int height, int levels) {
        if (!isValid(width, height, levels)) {
            throw new IllegalArgumentException("a texture has an area of up to " + MAX_EXTENT + " texels a side and a "
                    + "level for each halving of it: " + width + "x" + height + ", " + levels + " level(s)");
        }
        this.width = width;
        this.height = height;
        this.levels = levels;
    }

    public static boolean isValid(int width, int height, int levels) {
        boolean hasValidArea = width >= 1 && height >= 1 && width <= MAX_EXTENT && height <= MAX_EXTENT;
        return hasValidArea && levels >= 1 && levels <= fullChainOf(width, height);
    }

    public static int fullChainOf(int width, int height) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(width, height));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int levels() {
        return levels;
    }

    boolean holds(TexelArea area) {
        if (area.level() >= levels) {
            return false;
        }
        int levelWidth = Math.max(1, width >> area.level());
        int levelHeight = Math.max(1, height >> area.level());
        return area.x() < levelWidth
                && area.y() < levelHeight
                && area.width() <= levelWidth - area.x()
                && area.height() <= levelHeight - area.y();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TextureShape)) {
            return false;
        }
        TextureShape shape = (TextureShape) other;
        return width == shape.width && height == shape.height && levels == shape.levels;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * width + height) + levels;
    }

    @Override
    public String toString() {
        return width + "x" + height + " with " + levels + " level(s)";
    }
}
