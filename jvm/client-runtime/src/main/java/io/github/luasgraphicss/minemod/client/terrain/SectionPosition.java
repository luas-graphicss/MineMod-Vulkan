package io.github.luasgraphicss.minemod.client.terrain;

public final class SectionPosition {
    private static final int BLOCKS_PER_EDGE_SHIFT = 4;

    private final int x;
    private final int y;
    private final int z;

    public SectionPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static SectionPosition containingBlock(int blockX, int blockY, int blockZ) {
        return new SectionPosition(
                blockX >> BLOCKS_PER_EDGE_SHIFT, blockY >> BLOCKS_PER_EDGE_SHIFT, blockZ >> BLOCKS_PER_EDGE_SHIFT);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SectionPosition)) {
            return false;
        }
        SectionPosition position = (SectionPosition) other;
        return x == position.x && y == position.y && z == position.z;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * x + y) + z;
    }

    @Override
    public String toString() {
        return "section " + x + "," + y + "," + z;
    }
}
