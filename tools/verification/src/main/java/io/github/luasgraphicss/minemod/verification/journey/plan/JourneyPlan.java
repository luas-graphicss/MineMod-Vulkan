package io.github.luasgraphicss.minemod.verification.journey.plan;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record JourneyPlan(
        String name,
        int timeoutMinutes,
        int stepTimeoutSeconds,
        World world,
        Map<String, String> options,
        List<String> packs,
        List<JourneyStep> steps,
        List<Scene> scenes) {

    public JourneyPlan {
        options = Map.copyOf(options);
        packs = List.copyOf(packs);
        steps = List.copyOf(steps);
        scenes = List.copyOf(scenes);
    }

    public record World(String name, long seed) {}

    public record Scene(String capture, int cellSize, int channelTolerance, double differingCells, double meanError) {}

    public List<String> argumentsOf(JourneyStep.Kind kind) {
        return steps.stream()
                .filter(step -> step.kind() == kind)
                .map(JourneyStep::argument)
                .toList();
    }

    public Set<JourneyStep.Kind> kinds() {
        return steps.stream().map(JourneyStep::kind).collect(Collectors.toUnmodifiableSet());
    }
}
