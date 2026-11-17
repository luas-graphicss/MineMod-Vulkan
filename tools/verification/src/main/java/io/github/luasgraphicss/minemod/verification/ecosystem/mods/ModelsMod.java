package io.github.luasgraphicss.minemod.verification.ecosystem.mods;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.adapters.events.resources.ResourceExtensionPoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Priority;
import io.github.luasgraphicss.minemod.verification.ecosystem.session.ModContext;

public final class ModelsMod {
    private ModelsMod() {}

    public static void install(ModContext mod) {
        mod.onReload(() -> {
            mod.generations().complete(ResourceExtensionPoint.MODEL_BAKING);
            mod.generations().complete(ResourceExtensionPoint.ATLAS_STITCHING);
            mod.note("bakes its models and stitches its sprites");
        });
        mod.bus().on(FramePoint.AFTER_ENTITIES, Priority.NORMAL, false, canceled -> {
            if (!mod.generations().isCurrent(mod.clock().resourceGeneration())) {
                mod.fail("drew models of a resource generation that is not published");
            }
            mod.note("draws its models");
            return false;
        });
    }
}
