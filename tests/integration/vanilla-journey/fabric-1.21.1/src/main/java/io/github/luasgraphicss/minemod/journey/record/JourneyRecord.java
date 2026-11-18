package io.github.luasgraphicss.minemod.journey.record;

import io.github.luasgraphicss.minemod.journey.inspection.Inspection;
import java.util.ArrayList;
import java.util.List;

public final class JourneyRecord {
    private final String plan;
    private final String planSha256;
    private final List<String> mods;
    private final String openGlProvider;
    private final List<StepOutcome> steps = new ArrayList<>();
    private final List<Inspection> inspections = new ArrayList<>();
    private boolean finished;

    public JourneyRecord(String plan, String planSha256, List<String> mods, String openGlProvider) {
        this.plan = plan;
        this.planSha256 = planSha256;
        this.mods = List.copyOf(mods);
        this.openGlProvider = openGlProvider;
    }

    public void add(StepOutcome outcome) {
        steps.add(outcome);
    }

    public void add(Inspection inspection) {
        inspections.add(inspection);
    }

    public void finish() {
        finished = true;
    }
}
