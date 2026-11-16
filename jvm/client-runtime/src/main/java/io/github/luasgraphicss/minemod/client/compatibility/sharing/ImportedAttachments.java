package io.github.luasgraphicss.minemod.client.compatibility.sharing;

import io.github.luasgraphicss.minemod.bridge.sharing.SharedAttachments;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;

public final class ImportedAttachments {
    private final SharedAttachments shared;
    private final SharingPlan plan;
    private final SharedResource color;
    private final SharedResource depthStencil;

    ImportedAttachments(SharedAttachments shared, SharingPlan plan, SharedResource color, SharedResource depthStencil) {
        this.shared = shared;
        this.plan = plan;
        this.color = color;
        this.depthStencil = depthStencil;
    }

    public SharedAttachments shared() {
        return shared;
    }

    public SharingPlan plan() {
        return plan;
    }

    public SceneTextures textures() {
        return new SceneTextures(color.providerObject(), depthStencil.providerObject());
    }

    SharedResource color() {
        return color;
    }

    SharedResource depthStencil() {
        return depthStencil;
    }
}
