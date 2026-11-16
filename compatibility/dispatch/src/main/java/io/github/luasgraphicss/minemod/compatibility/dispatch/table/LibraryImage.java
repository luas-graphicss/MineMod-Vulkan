package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

public final class LibraryImage {
    private final long[] bounds;

    private LibraryImage(long[] bounds) {
        this.bounds = bounds;
    }

    public static LibraryImage ofSegments(long... startsAndEnds) {
        if (startsAndEnds.length == 0 || startsAndEnds.length % 2 != 0) {
            throw new IllegalArgumentException("a library image is one or more segments, each with a start and an end");
        }
        for (int index = 0; index < startsAndEnds.length; index += 2) {
            if (Long.compareUnsigned(startsAndEnds[index], startsAndEnds[index + 1]) >= 0) {
                throw new IllegalArgumentException("a segment ends after it starts");
            }
        }
        return new LibraryImage(startsAndEnds.clone());
    }

    public boolean contains(long address) {
        for (int index = 0; index < bounds.length; index += 2) {
            if (Long.compareUnsigned(address, bounds[index]) >= 0
                    && Long.compareUnsigned(address, bounds[index + 1]) < 0) {
                return true;
            }
        }
        return false;
    }
}
