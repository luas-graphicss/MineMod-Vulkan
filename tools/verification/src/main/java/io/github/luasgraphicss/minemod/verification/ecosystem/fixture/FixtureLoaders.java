package io.github.luasgraphicss.minemod.verification.ecosystem.fixture;

import io.github.luasgraphicss.minemod.adapters.events.contract.LoaderEventContract;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import io.github.luasgraphicss.minemod.verification.ecosystem.fixture.FixtureBus.Dispatch;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;

public final class FixtureLoaders {
    private FixtureLoaders() {}

    public static Dispatch dispatchOf(LoaderTarget.Kind loader) {
        return switch (loader) {
            case FORGE, NEOFORGE -> Dispatch.PRIORITY_BUS;
            case FABRIC -> Dispatch.CALLBACK_ARRAY;
            case CLIENT_BOOTSTRAP -> throw new IllegalArgumentException("the client bootstrap loads no mods");
        };
    }

    public static LoaderEventContract contractOf(LoaderTarget.Kind loader) {
        LoaderEventContract.Builder contract = LoaderEventContract.of("fixture " + dispatchOf(loader))
                .notifies(
                        FramePoint.FRAME_START,
                        FramePoint.AFTER_ENTITIES,
                        FramePoint.AFTER_TRANSLUCENT_TERRAIN,
                        FramePoint.WORLD_LAST,
                        FramePoint.AFTER_HUD,
                        FramePoint.FRAME_END)
                .cancels(FramePoint.BEFORE_BLOCK_OUTLINE);
        if (dispatchOf(loader) == Dispatch.PRIORITY_BUS) {
            contract.notifies(FramePoint.AFTER_SKY)
                    .cancels(FramePoint.BEFORE_HAND, FramePoint.BEFORE_HUD)
                    .notifiesElementsAt(FramePoint.HUD_ELEMENTS);
        } else {
            contract.notifies(FramePoint.AFTER_SETUP, FramePoint.BEFORE_ENTITIES);
        }
        return contract.build();
    }
}
