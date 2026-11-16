package io.github.luasgraphicss.minemod.contract.handle;

public final class ResourceHandle {
    public static final int MAX_GENERATION = (1 << 24) - 1;

    private static final int INDEX_BITS = 32;
    private static final int KIND_SHIFT = 56;
    private static final long INDEX_MASK = 0xFFFFFFFFL;

    private final long bits;

    private ResourceHandle(long bits) {
        this.bits = bits;
    }

    public static ResourceHandle of(ResourceKind kind, int generation, int index) {
        if (generation < 1 || generation > MAX_GENERATION) {
            throw new IllegalArgumentException("handle generation out of range: " + generation);
        }
        if (index < 0) {
            throw new IllegalArgumentException("handle index out of range: " + index);
        }
        return new ResourceHandle(((long) kind.code() << KIND_SHIFT) | ((long) generation << INDEX_BITS) | index);
    }

    public static ResourceHandle fromBits(long bits) {
        ResourceKind kind = ResourceKind.fromCode((int) (bits >>> KIND_SHIFT));
        long index = bits & INDEX_MASK;
        if (index > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("handle index out of range: " + index);
        }
        return of(kind, (int) (bits >>> INDEX_BITS) & MAX_GENERATION, (int) index);
    }

    public long bits() {
        return bits;
    }

    public ResourceKind kind() {
        return ResourceKind.fromCode((int) (bits >>> KIND_SHIFT));
    }

    public int generation() {
        return (int) (bits >>> INDEX_BITS) & MAX_GENERATION;
    }

    public int index() {
        return (int) (bits & INDEX_MASK);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ResourceHandle && ((ResourceHandle) other).bits == bits;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bits);
    }

    @Override
    public String toString() {
        return kind() + "#" + index() + "@" + generation();
    }
}
