package io.github.luasgraphicss.minemod.verification.journey.plan;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PlannedDimensions {
    public static final String OVERWORLD = "minecraft:overworld";

    private PlannedDimensions() {}

    public static Map<String, Optional<String>> atInspections(JourneyPlan plan) {
        Map<String, Optional<String>> dimensions = new LinkedHashMap<>();
        String dimension = OVERWORLD;
        boolean isInWorld = false;
        for (JourneyStep step : plan.steps()) {
            switch (step.kind()) {
                case CREATE_WORLD -> {
                    dimension = OVERWORLD;
                    isInWorld = true;
                }
                case OPEN_WORLD -> isInWorld = true;
                case LEAVE_WORLD -> isInWorld = false;
                case EXPECT_DIMENSION -> dimension = step.argument();
                case INSPECT -> dimensions.put(step.argument(), isInWorld ? Optional.of(dimension) : Optional.empty());
                default -> {}
            }
        }
        return dimensions;
    }
}
