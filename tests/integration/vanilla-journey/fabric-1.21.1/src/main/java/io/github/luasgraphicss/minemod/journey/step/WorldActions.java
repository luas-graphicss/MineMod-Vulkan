package io.github.luasgraphicss.minemod.journey.step;

import io.github.luasgraphicss.minemod.journey.plan.JourneyPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

final class WorldActions {
    private static final String SAVING_LEVEL = "menu.savingLevel";

    private WorldActions() {}

    static StepAction waitForTitle() {
        return StepAction.until(minecraft -> {}, WorldActions::showsTheTitle);
    }

    static StepAction create(JourneyPlan.World world) {
        return StepAction.until(
                minecraft -> {
                    LevelSettings settings = new LevelSettings(
                            world.name(),
                            GameType.CREATIVE,
                            false,
                            Difficulty.PEACEFUL,
                            true,
                            new GameRules(),
                            WorldDataConfiguration.DEFAULT);
                    WorldOptions options = new WorldOptions(world.seed(), false, false);
                    minecraft
                            .createWorldOpenFlows()
                            .createFreshLevel(
                                    world.name(),
                                    settings,
                                    options,
                                    WorldPresets::createNormalWorldDimensions,
                                    minecraft.screen);
                },
                WorldActions::isInTheWorld);
    }

    static StepAction open(JourneyPlan.World world) {
        return StepAction.until(
                minecraft -> minecraft
                        .createWorldOpenFlows()
                        .openWorld(world.name(), () -> minecraft.setScreen(new TitleScreen())),
                WorldActions::isInTheWorld);
    }

    static StepAction leave() {
        return StepAction.until(
                minecraft -> {
                    requireWorld(minecraft);
                    minecraft.level.disconnect();
                    minecraft.disconnect(new GenericMessageScreen(Component.translatable(SAVING_LEVEL)));
                    minecraft.setScreen(new TitleScreen());
                },
                minecraft -> minecraft.level == null && !minecraft.hasSingleplayerServer() && showsTheTitle(minecraft));
    }

    static StepAction expectDimension(String dimension) {
        return StepAction.until(
                minecraft -> {},
                minecraft -> isInTheWorld(minecraft)
                        && minecraft.level.dimension().location().toString().equals(dimension));
    }

    static void requireWorld(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) {
            throw new IllegalStateException("the step needs an open world");
        }
    }

    private static boolean showsTheTitle(Minecraft minecraft) {
        return minecraft.getOverlay() == null && minecraft.screen instanceof TitleScreen;
    }

    private static boolean isInTheWorld(Minecraft minecraft) {
        return minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && minecraft.getOverlay() == null;
    }
}
