package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import io.github.luasgraphicss.minemod.verification.journey.evidence.LoadedLibraries;
import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererLines;
import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererReport;
import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
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
import java.util.List;
import java.util.Optional;

public final class VanillaWithoutGlScenario implements TargetScenario {
    private static final String NAME = "vanilla-without-gl";

    private final JourneyRunner runner;

    public VanillaWithoutGlScenario(InstanceLauncher launcher) {
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
        Optional<JourneyEvidence> candidate = runner.run(environment, target, NAME, GameInstance.CANDIDATE, log);
        if (candidate.isEmpty()) {
            return log.report();
        }
        verifyCandidate(candidate.get(), log);
        runner.run(environment, target, NAME, GameInstance.REFERENCE, log)
                .ifPresent(reference -> verifyReference(reference, log));
        return log.report();
    }

    private static void verifyCandidate(JourneyEvidence evidence, ScenarioLog log) {
        String instance = evidence.subject();
        JourneyRules.findViolations(evidence).forEach(violation -> log.fail(instance + violation));
        JourneyRules.findCandidateViolations(evidence.record()).forEach(violation -> log.fail(instance + violation));
        log.expect(!evidence.record().inspections().isEmpty(), instance + "at least one inspection");
        for (Inspection inspection : evidence.record().inspections()) {
            String subject = instance + NativeRendering.subjectOf(inspection);
            Optional<LoadedLibraries> libraries = inspection.loadedLibraries();
            if (libraries.isEmpty()) {
                log.fail(subject + "the libraries of the process were not read, which only Linux allows");
                continue;
            }
            log.expectEqual(List.of(), libraries.get().openGl(), subject + "OpenGL libraries in the process");
            log.expect(
                    !libraries.get().has(LoadedLibraries.OPENGL_PROVIDER),
                    subject + "a process without the OpenGL provider of the compatibility layer");
            log.expect(
                    libraries.get().has(LoadedLibraries.NATIVE_RENDERER) && libraries.get().hasVulkanLoader(),
                    subject + "the native renderer and the Vulkan loader in the process");
            Optional<RendererReport> report = NativeRendering.expectFrames(inspection, log);
            Optional<RendererLines> lines = inspection.rendererLines();
            if (report.isEmpty() || lines.isEmpty()) {
                log.expect(lines.isPresent(), subject + "the lines of the native renderer on the debug screen");
                continue;
            }
            log.expect(
                    report.get().peakBytesOf(RendererReport.TEXTURES) > 0,
                    subject + "textures uploaded to the native renderer");
            if (inspection.world().isPresent()) {
                log.expect(
                        report.get().peakBytesOf(RendererReport.BUFFERS) > 0,
                        subject + "buffers uploaded to the native renderer");
            }
            log.expectEqual(0L, lines.get().leftOutDraws(), subject + "draws of the game left out of the native scene");
            log.observe(subject + "no OpenGL library among " + libraries.get().names().size() + " in the process, "
                    + report.get().frames() + " frame(s) measured, " + report.get().drawsIn(RendererReport.WORLD_PASS)
                    + " draw(s) in the world pass, " + report.get().peakBytesOf(RendererReport.TEXTURES)
                    + " byte(s) of textures and " + report.get().peakBytesOf(RendererReport.BUFFERS)
                    + " byte(s) of buffers at their peak, no draw left out");
        }
    }

    private static void verifyReference(JourneyEvidence evidence, ScenarioLog log) {
        String instance = evidence.subject();
        JourneyRules.findViolations(evidence).forEach(violation -> log.fail(instance + violation));
        log.expect(
                !evidence.record().ranWith(JourneyRules.CANDIDATE_MOD),
                instance + "the game without the " + JourneyRules.CANDIDATE_MOD + " mod");
        for (Inspection inspection : evidence.record().inspections()) {
            String subject = instance + NativeRendering.subjectOf(inspection);
            Optional<LoadedLibraries> libraries = inspection.loadedLibraries();
            if (libraries.isEmpty() || libraries.get().openGl().isEmpty()) {
                log.fail(subject + "no OpenGL library was seen in the game that draws with OpenGL, so the same"
                        + " inspection proves nothing about the candidate");
                continue;
            }
            log.observe(subject + "the game with its own renderer maps " + libraries.get().openGl());
        }
    }
}
