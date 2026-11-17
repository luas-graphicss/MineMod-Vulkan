package io.github.luasgraphicss.minemod.journey.plan;

import java.util.List;

public record JourneyPlan(String name, int stepTimeoutSeconds, World world, List<PlanStep> steps) {
    public record World(String name, long seed) {}
}
