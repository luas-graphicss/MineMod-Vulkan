package io.github.luasgraphicss.minemod.journey.record;

import io.github.luasgraphicss.minemod.journey.plan.PlanStep;

public record StepOutcome(int index, String kind, String argument, String status, String detail) {
    private static final String COMPLETED = "completed";
    private static final String FAILED = "failed";
    private static final String TIMED_OUT = "timed-out";

    public static StepOutcome completed(int index, PlanStep step) {
        return new StepOutcome(index, step.kind(), step.argument(), COMPLETED, "");
    }

    public static StepOutcome failed(int index, PlanStep step, String detail) {
        return new StepOutcome(index, step.kind(), step.argument(), FAILED, detail);
    }

    public static StepOutcome timedOut(int index, PlanStep step, long seconds) {
        return new StepOutcome(index, step.kind(), step.argument(), TIMED_OUT, "not done after " + seconds + " s");
    }

    public boolean isCompleted() {
        return status.equals(COMPLETED);
    }
}
