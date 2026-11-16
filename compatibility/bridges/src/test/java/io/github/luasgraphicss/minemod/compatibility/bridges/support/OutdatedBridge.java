package io.github.luasgraphicss.minemod.compatibility.bridges.support;

import io.github.luasgraphicss.minemod.compatibility.spi.SpiVersion;
import java.util.ArrayList;

public final class OutdatedBridge extends RecordingBridge {
    public OutdatedBridge() {
        super("outdated", SpiVersion.CURRENT + 1, new ArrayList<String>(), "transform feedback across native draws");
    }
}
