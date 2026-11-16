package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.DispatchTable;
import io.github.luasgraphicss.minemod.compatibility.dispatch.table.LibraryImage;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;

public final class SimulatedDispatch {
    private static final long PROVIDER_IMAGE_START = 0x7F00_0000_0000L;
    private static final long PROVIDER_IMAGE_END = 0x7F00_0100_0000L;
    private static final long THUNKS_START = 0x5500_0000_0000L;
    private static final int ADDRESS_SLOTS = 0x10000;
    private static final int ADDRESS_STRIDE = 16;

    private SimulatedDispatch() {}

    public static DispatchTable table(CallCatalog catalog, ContextProfile profile) {
        return new DispatchTable(
                catalog,
                profile,
                function -> PROVIDER_IMAGE_START + addressOffset(function),
                LibraryImage.ofSegments(PROVIDER_IMAGE_START, PROVIDER_IMAGE_END),
                function -> THUNKS_START + addressOffset(function));
    }

    private static long addressOffset(String function) {
        return (long) ADDRESS_STRIDE * (Math.floorMod(function.hashCode(), ADDRESS_SLOTS) + 1);
    }
}
