package io.github.luasgraphicss.minemod.bridge;

import io.github.luasgraphicss.minemod.contract.capability.Capability;
import java.nio.file.Paths;
import java.util.EnumSet;

final class LoadedBridge {
    static final NativeBridge BRIDGE =
            NativeBridge.load(Paths.get(System.getProperty("minemod.nativeLibrary")), EnumSet.allOf(Capability.class));

    private LoadedBridge() {}
}
