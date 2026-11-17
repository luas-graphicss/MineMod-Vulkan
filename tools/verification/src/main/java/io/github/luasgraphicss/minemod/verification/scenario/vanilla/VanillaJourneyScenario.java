package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererReport;
import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep.Kind;
import io.github.luasgraphicss.minemod.verification.journey.plan.PlannedDimensions;
import io.github.luasgraphicss.minemod.verification.journey.record.Inspection;
import io.github.luasgraphicss.minemod.verification.journey.run.JourneyEvidence;
import io.github.luasgraphicss.minemod.verification.journey.run.JourneyRules;
import io.github.luasgraphicss.minemod.verification.journey.run.JourneyRunner;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.TargetScenario;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class VanillaJourneyScenario implements TargetScenario {
    private static final String NAME = "vanilla-journey";
    private static final String NO_WORLD = "no world";
    private static final Set<Kind> LEGS = EnumSet.of(
            Kind.CREATE_WORLD, Kind.EXPECT_DIMENSION, Kind.RELOAD, Kind.LEAVE_WORLD, Kind.OPEN_WORLD, Kind.QUIT);
    private static final Set<String> DIMENSIONS =
            Set.of(PlannedDimensions.OVERWORLD, "minecraft:the_nether", "minecraft:the_end");

    private final JourneyRunner runner;

    public VanillaJourneyScenario(InstanceLauncher launcher) {
        this.runner = new JourneyRunner(launcher);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment, GameTarget target)
            throws IOException, InterruptedException {
        ScenarioLog log = new ScenarioLog();
        runner.run(environment, target, NAME, GameInstance.CANDIDATE, log).ifPresent(evidence -> verify(evidence, log));
        return log.report();
    }

    private static void verify(JourneyEvidence evidence, ScenarioLog log) {
        JourneyRules.findViolations(evidence).forEach(log::fail);
        JourneyRules.findCandidateViolations(evidence.record()).forEach(log::fail);
        JourneyPlan plan = evidence.plan();
        List<String> missingLegs = LEGS.stream()
                .filter(leg -> !plan.kinds().contains(leg))
                .map(Kind::label)
                .toList();
        log.expect(missingLegs.isEmpty(), "a plan with every leg of the journey; it has no " + missingLegs);

        Map<String, Optional<String>> planned = PlannedDimensions.atInspections(plan);
        Set<String> inspected = new TreeSet<>();
        for (Inspection inspection : evidence.record().inspections()) {
            String subject = NativeRendering.subjectOf(inspection);
            String world = inspection.world().orElse(NO_WORLD);
            log.expectEqual(
                    planned.getOrDefault(inspection.name(), Optional.empty()).orElse(NO_WORLD),
                    world,
                    subject + "dimension");
            inspection.world().ifPresent(inspected::add);
            NativeRendering.expectFrames(inspection, log)
                    .ifPresent(report -> log.observe(subject + world + ", " + report.frames()
                            + " frame(s) measured and " + report.drawsIn(RendererReport.WORLD_PASS)
                            + " draw(s) in the world pass of the native renderer"));
        }
        log.expect(
                inspected.containsAll(DIMENSIONS),
                "inspections in " + new TreeSet<>(DIMENSIONS) + ", found " + inspected);
        log.observe("the game created world " + plan.world().name() + " with seed " + plan.world().seed()
                + ", was inspected in " + inspected + ", reloaded its resources "
                + plan.argumentsOf(Kind.RELOAD).size()
                + " time(s), left the world, opened it again and ended with exit code "
                + evidence.exit().exitCode() + " without the compatibility provider, with "
                + evidence.record().mods());
    }
}
