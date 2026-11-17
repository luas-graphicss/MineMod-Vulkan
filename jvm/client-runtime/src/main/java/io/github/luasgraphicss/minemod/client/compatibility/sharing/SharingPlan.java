package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachment;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;

public final class SharingPlan {
    private final PlannedShare color;
    private final PlannedShare depthStencil;
    private final HandoffSynchronization synchronization;

    SharingPlan(PlannedShare color, PlannedShare depthStencil, HandoffSynchronization synchronization) {
        this.color = color;
        this.depthStencil = depthStencil;
        this.synchronization = synchronization;
    }

    public PlannedShare share(SharedAttachment attachment) {
        return attachment == SharedAttachment.COLOR ? color : depthStencil;
    }

    public HandoffSynchronization synchronization() {
        return synchronization;
    }
}
