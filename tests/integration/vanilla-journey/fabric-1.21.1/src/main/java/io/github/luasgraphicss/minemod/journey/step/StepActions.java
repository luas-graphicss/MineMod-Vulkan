package io.github.luasgraphicss.minemod.journey.step;

import io.github.luasgraphicss.minemod.journey.inspection.InspectionProbe;
import io.github.luasgraphicss.minemod.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.journey.plan.PlanStep;
import io.github.luasgraphicss.minemod.journey.record.JourneyRecord;
import java.nio.file.Path;

public final class StepActions {
    public static final String QUIT = "quit";

    private final JourneyPlan plan;
    private final JourneyRecord record;
    private final Path output;

    public StepActions(JourneyPlan plan, JourneyRecord record, Path output) {
        this.plan = plan;
        this.record = record;
        this.output = output;
    }

    public StepAction of(PlanStep step) {
        String argument = step.argument();
        return switch (step.kind()) {
            case "wait-for-title" -> WorldActions.waitForTitle();
            case "create-world" -> WorldActions.create(plan.world());
            case "open-world" -> WorldActions.open(plan.world());
            case "leave-world" -> WorldActions.leave();
            case "expect-dimension" -> WorldActions.expectDimension(argument);
            case "command" -> CommandAction.required(argument);
            case "attempt" -> CommandAction.attempted(argument);
            case "wait" -> ViewActions.waitFor(Long.parseLong(argument));
            case "settle" -> ViewActions.settle(Long.parseLong(argument));
            case "select-packs" -> ResourceActions.select(argument);
            case "reload" -> ResourceActions.reload();
            case "hud" -> ViewActions.hud(argument);
            case "screen" -> ViewActions.screen(argument);
            case "capture" -> ViewActions.capture(output, argument);
            case "inspect" ->
                StepAction.immediate(minecraft -> record.add(InspectionProbe.inspect(minecraft, argument)));
            case QUIT -> StepAction.immediate(minecraft -> {});
            default -> throw new IllegalArgumentException("the driver has no step named " + step.kind());
        };
    }
}
