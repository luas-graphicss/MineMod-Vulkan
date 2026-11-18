package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererLines;
import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererLines.Programs;
import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererReport;
import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep.Kind;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ResourceReloadScenario implements TargetScenario {
    private static final String NAME = "resource-reload";
    private static final String BASELINE = "baseline";
    private static final String REPEATED = "repeated";
    private static final String REPLACED = "replaced";
    private static final String REFUSED = "refused";
    private static final String RESTORED = "restored";
    private static final List<String> INSPECTIONS = List.of(BASELINE, REPEATED, REPLACED, REFUSED, RESTORED);

    private final JourneyRunner runner;

    public ResourceReloadScenario(InstanceLauncher launcher) {
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
        Map<String, RendererLines> described = new LinkedHashMap<>();
        for (String name : INSPECTIONS) {
            Optional<Inspection> inspection = evidence.record().inspection(name);
            if (inspection.isEmpty()) {
                log.fail("the journey recorded no inspection named " + name);
                continue;
            }
            NativeRendering.expectFrames(inspection.get(), log);
            Optional<RendererLines> lines = inspection.get().rendererLines();
            if (lines.isEmpty()) {
                log.fail(NativeRendering.subjectOf(inspection.get())
                        + "the debug screen has no lines of the native renderer");
                continue;
            }
            described.put(name, lines.get());
        }
        if (described.size() < INSPECTIONS.size()) {
            return;
        }

        expectPrograms(described, BASELINE, Programs.BUILT_IN, log);
        expectPrograms(described, REPEATED, Programs.BUILT_IN, log);
        expectPrograms(described, REPLACED, Programs.REPLACED, log);
        expectPrograms(described, REFUSED, Programs.REFUSED, log);
        expectPrograms(described, RESTORED, Programs.BUILT_IN, log);
        log.expect(
                described.get(REPLACED).shaders().replaced() > 0,
                "at least one program replaced by the valid pack, found "
                        + described.get(REPLACED).shaders().replaced());
        long generation = described.get(BASELINE).generation();
        for (String name : INSPECTIONS) {
            long reached = described.get(name).generation();
            log.expect(reached >= generation, "inspection " + name + ": a generation of at least " + generation
                    + ", found " + reached);
            generation = Math.max(generation, reached);
        }
        log.expectEqual(
                described.get(REPLACED).generation(),
                described.get(REFUSED).generation(),
                "generation kept by the reload the renderer refused");
        boolean explainsTheRefusal = evidence.record()
                .inspection(REFUSED)
                .flatMap(Inspection::renderer)
                .flatMap(RendererReport::refusedReload)
                .filter(refusal -> !refusal.isBlank())
                .isPresent();
        log.expect(explainsTheRefusal, "the report of the renderer to say why it refused the reload");
        log.expectEqual(
                0L,
                described.get(RESTORED).retiringResources(),
                "resources still retiring once the last reload settled");

        described.forEach((name, lines) -> log.observe("inspection " + name + ": generation " + lines.generation()
                + ", " + describe(lines.shaders()) + ", " + lines.retiringResources() + " resource(s) retiring"));
        log.observe("the game reloaded its resources " + evidence.plan().argumentsOf(Kind.RELOAD).size()
                + " time(s); the refused reload kept generation " + described.get(REFUSED).generation()
                + " and frames kept being measured after it");
    }

    private static void expectPrograms(
            Map<String, RendererLines> described, String inspection, Programs expected, ScenarioLog log) {
        log.expectEqual(
                expected, described.get(inspection).shaders().programs(), "inspection " + inspection + ": programs");
    }

    private static String describe(RendererLines.Shaders shaders) {
        return switch (shaders.programs()) {
            case BUILT_IN -> "built-in programs";
            case REPLACED -> shaders.replaced() + " program(s) replaced by resource packs";
            case REFUSED -> "reload refused (" + shaders.refusal() + ")";
        };
    }
}
