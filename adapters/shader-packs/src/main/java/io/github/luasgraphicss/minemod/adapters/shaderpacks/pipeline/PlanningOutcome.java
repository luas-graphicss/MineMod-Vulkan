package io.github.luasgraphicss.minemod.adapters.shaderpacks.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class PlanningOutcome {
    private final FramePlan plan;
    private final List<String> problems;

    private PlanningOutcome(FramePlan plan, Collection<String> problems) {
        this.plan = plan;
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    static PlanningOutcome of(FramePlan plan, Collection<String> problems) {
        return new PlanningOutcome(problems.isEmpty() ? plan : null, problems);
    }

    public Optional<FramePlan> plan() {
        return Optional.ofNullable(plan);
    }

    public List<String> problems() {
        return problems;
    }
}
