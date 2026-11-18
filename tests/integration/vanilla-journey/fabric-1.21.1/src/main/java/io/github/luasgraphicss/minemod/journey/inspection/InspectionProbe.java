package io.github.luasgraphicss.minemod.journey.inspection;

import io.github.luasgraphicss.minemod.journey.mixin.DebugScreenOverlayInvoker;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class InspectionProbe {
    private static final long NO_GAME_TIME = -1;

    private InspectionProbe() {}

    public static Inspection inspect(Minecraft minecraft, String name) {
        ClientLevel level = minecraft.level;
        DebugScreenOverlayInvoker debugScreen = (DebugScreenOverlayInvoker) minecraft.getDebugOverlay();
        return new Inspection(
                name,
                level == null ? null : level.dimension().location().toString(),
                level == null ? NO_GAME_TIME : level.getGameTime(),
                List.copyOf(debugScreen.journey$systemInformation()),
                RendererDetails.reportedOrNull(minecraft),
                LoadedLibraries.namesOrNull(),
                List.copyOf(minecraft.getResourcePackRepository().getSelectedIds()));
    }
}
