package io.github.luasgraphicss.minemod.contract.abi;

public final class AbiVersion {
    public static final AbiVersion CURRENT = new AbiVersion(0, 1);

    private static final int MAX_COMPONENT = 0xFFFF;

    private final int major;
    private final int minor;

    public AbiVersion(int major, int minor) {
        if (major < 0 || major > MAX_COMPONENT || minor < 0 || minor > MAX_COMPONENT) {
            throw new IllegalArgumentException("ABI version components must fit 16 bits: " + major + "." + minor);
        }
        this.major = major;
        this.minor = minor;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public boolean accepts(AbiVersion client) {
        if (client.major != major) {
            return false;
        }
        boolean unstableGeneration = major == 0;
        return unstableGeneration ? client.minor == minor : client.minor <= minor;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AbiVersion && ((AbiVersion) other).major == major && ((AbiVersion) other).minor == minor;
    }

    @Override
    public int hashCode() {
        return (major << 16) | minor;
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
