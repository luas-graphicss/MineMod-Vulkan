package io.github.luasgraphicss.minemod.journey;

import io.github.luasgraphicss.minemod.journey.driver.JourneyDriver;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;

public final class JourneyClient implements ClientModInitializer {
    private static final String PLAN_PROPERTY = "minemod.journey.plan";
    private static final String OUTPUT_PROPERTY = "minemod.journey.output";

    @Override
    public void onInitializeClient() {
        String plan = System.getProperty(PLAN_PROPERTY);
        String output = System.getProperty(OUTPUT_PROPERTY);
        if (plan != null && output != null) {
            JourneyDriver.start(Path.of(plan), Path.of(output));
        }
    }
}
