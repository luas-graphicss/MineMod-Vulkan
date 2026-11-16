package io.github.luasgraphicss.minemod.compatibility.bridges.support;

import java.util.ArrayList;

public final class DiscoveredBridge extends RecordingBridge {
    public static final String ID = "discovered";
    public static final String OPERATION = "glReadBuffer on the front buffer";

    public DiscoveredBridge() {
        super(ID, new ArrayList<String>(), OPERATION);
    }
}
