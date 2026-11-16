package io.github.luasgraphicss.minemod.compatibility.spi;

public final class SpiVersion {
    public static final int CURRENT = 1;

    private SpiVersion() {
    }

    public static boolean accepts(int bridgeVersion) {
        return bridgeVersion == CURRENT;
    }
}
