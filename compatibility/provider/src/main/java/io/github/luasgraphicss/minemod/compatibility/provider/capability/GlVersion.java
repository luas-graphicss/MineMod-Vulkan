package io.github.luasgraphicss.minemod.compatibility.provider.capability;

public final class GlVersion {
    private final int major;
    private final int minor;

    private GlVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public static GlVersion of(int major, int minor) {
        if (major < 1 || minor < 0) {
            throw new IllegalArgumentException("not an OpenGL version: " + major + "." + minor);
        }
        return new GlVersion(major, minor);
    }

    public boolean isAtLeast(GlVersion other) {
        return major > other.major || (major == other.major && minor >= other.minor);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GlVersion)) {
            return false;
        }
        GlVersion version = (GlVersion) other;
        return major == version.major && minor == version.minor;
    }

    @Override
    public int hashCode() {
        return major * 31 + minor;
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
