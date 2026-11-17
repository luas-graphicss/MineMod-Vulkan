package io.github.luasgraphicss.minemod.compatibility.bridges.activation;

import io.github.luasgraphicss.minemod.compatibility.bridges.contract.BridgeContract;
import io.github.luasgraphicss.minemod.compatibility.spi.RenderBridge;
import java.util.Objects;

public final class BridgeCandidate {
    private final RenderBridge bridge;
    private final BridgeContract contract;

    public BridgeCandidate(RenderBridge bridge, BridgeContract contract) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.contract = Objects.requireNonNull(contract, "contract");
    }

    public RenderBridge bridge() {
        return bridge;
    }

    public BridgeContract contract() {
        return contract;
    }
}
