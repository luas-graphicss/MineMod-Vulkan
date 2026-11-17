package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

import java.util.Objects;

public final class PackTarget implements Comparable<PackTarget> {
    public enum Kind {
        COLOR("colortex", 16),
        DEPTH("depthtex", 3),
        SHADOW_DEPTH("shadowtex", 2),
        SHADOW_COLOR("shadowcolor", 2);

        private final String samplerPrefix;
        private final int count;

        Kind(String samplerPrefix, int count) {
            this.samplerPrefix = samplerPrefix;
            this.count = count;
        }

        public int count() {
            return count;
        }
    }

    private final Kind kind;
    private final int index;

    private PackTarget(Kind kind, int index) {
        if (index < 0 || index >= kind.count) {
            throw new IllegalArgumentException(kind.samplerPrefix + index + " is not a target of a shader pack");
        }
        this.kind = kind;
        this.index = index;
    }

    public static PackTarget color(int index) {
        return new PackTarget(Kind.COLOR, index);
    }

    public static PackTarget depth(int index) {
        return new PackTarget(Kind.DEPTH, index);
    }

    public static PackTarget shadowDepth(int index) {
        return new PackTarget(Kind.SHADOW_DEPTH, index);
    }

    public static PackTarget shadowColor(int index) {
        return new PackTarget(Kind.SHADOW_COLOR, index);
    }

    public Kind kind() {
        return kind;
    }

    public int index() {
        return index;
    }

    public boolean flips() {
        return kind == Kind.COLOR || kind == Kind.SHADOW_COLOR;
    }

    @Override
    public int compareTo(PackTarget other) {
        int byKind = kind.compareTo(other.kind);
        return byKind != 0 ? byKind : Integer.compare(index, other.index);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PackTarget)) {
            return false;
        }
        PackTarget target = (PackTarget) other;
        return kind == target.kind && index == target.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, index);
    }

    @Override
    public String toString() {
        return kind.samplerPrefix + index;
    }
}
