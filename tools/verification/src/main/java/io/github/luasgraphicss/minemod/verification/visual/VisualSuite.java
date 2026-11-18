package io.github.luasgraphicss.minemod.verification.visual;

import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceDirectory;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan.Scene;
import io.github.luasgraphicss.minemod.verification.journey.run.JourneyEvidence;
import io.github.luasgraphicss.minemod.verification.journey.run.JourneyRules;
import io.github.luasgraphicss.minemod.verification.journey.run.JourneyRunner;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import javax.imageio.ImageIO;

public final class VisualSuite {
    public static final String NAME = "vanilla-visual";

    private static final String DIFFERENCES = "differences";
    private static final String IMAGE_EXTENSION = ".png";

    private final JourneyRunner runner;

    public VisualSuite(InstanceLauncher launcher) {
        this.runner = new JourneyRunner(launcher);
    }

    public VisualResult run(ScenarioEnvironment environment) throws IOException, InterruptedException {
        ScenarioLog log = new ScenarioLog();
        GameTarget target = GameTarget.reference();
        Optional<JourneyEvidence> candidate = runner.run(environment, target, NAME, GameInstance.CANDIDATE, log);
        Optional<JourneyEvidence> reference = candidate.isPresent()
                ? runner.run(environment, target, NAME, GameInstance.REFERENCE, log)
                : Optional.empty();
        if (candidate.isEmpty() || reference.isEmpty()) {
            return resultOf(0, log.report());
        }
        List<String> unfitInstances = findUnfitInstances(reference.get(), candidate.get());
        unfitInstances.forEach(log::fail);
        if (!unfitInstances.isEmpty()) {
            return resultOf(0, log.report());
        }
        JourneyPlan plan = candidate.get().plan();
        log.observe("plan " + plan.name() + ": world " + plan.world().name() + " with seed " + plan.world().seed()
                + ", options " + new TreeMap<>(plan.options()));
        log.observe("reference: " + reference.get().record().mods());
        log.observe("candidate: " + candidate.get().record().mods());
        for (Scene scene : plan.scenes()) {
            compare(scene, reference.get(), candidate.get(), log);
        }
        return resultOf(plan.scenes().size(), log.report());
    }

    private static List<String> findUnfitInstances(JourneyEvidence reference, JourneyEvidence candidate) {
        List<String> violations = new ArrayList<>();
        JourneyRules.findViolations(reference).forEach(violation -> violations.add(reference.subject() + violation));
        JourneyRules.findViolations(candidate).forEach(violation -> violations.add(candidate.subject() + violation));
        JourneyRules.findCandidateViolations(candidate.record())
                .forEach(violation -> violations.add(candidate.subject() + violation));
        if (reference.record().ranWith(JourneyRules.CANDIDATE_MOD)) {
            violations.add("expected a reference that draws with the renderer of the game, without the "
                    + JourneyRules.CANDIDATE_MOD + " mod");
        }
        return violations;
    }

    private static void compare(Scene scene, JourneyEvidence reference, JourneyEvidence candidate, ScenarioLog log)
            throws IOException {
        String subject = "scene " + scene.capture() + ": ";
        Optional<BufferedImage> expected = captureOf(reference, scene, log);
        Optional<BufferedImage> drawn = captureOf(candidate, scene, log);
        if (expected.isEmpty() || drawn.isEmpty()) {
            return;
        }
        SceneVerdict verdict = CaptureComparison.compare(expected.get(), drawn.get(), scene);
        if (verdict.matches()) {
            log.observe(String.format(
                    Locale.ROOT,
                    "%s%dx%d, mean error %.2f of 255, %.2f%% of the cells differ",
                    subject,
                    drawn.get().getWidth(),
                    drawn.get().getHeight(),
                    verdict.meanError(),
                    verdict.differingCells() * 100));
            return;
        }
        verdict.differences().forEach(difference -> log.fail(subject + difference));
        if (expected.get().getWidth() == drawn.get().getWidth()
                && expected.get().getHeight() == drawn.get().getHeight()) {
            Path marked = candidate.directory().resolve(DIFFERENCES).resolve(scene.capture() + IMAGE_EXTENSION);
            Files.createDirectories(marked.getParent());
            DifferenceImage.write(expected.get(), drawn.get(), scene, marked);
            log.fail(subject + "the cells that differ are marked in " + marked);
        }
    }

    private static Optional<BufferedImage> captureOf(JourneyEvidence evidence, Scene scene, ScenarioLog log)
            throws IOException {
        Path capture = InstanceDirectory.captureIn(evidence.directory(), scene.capture());
        BufferedImage image = Files.isRegularFile(capture) ? ImageIO.read(capture.toFile()) : null;
        if (image == null) {
            log.fail("scene " + scene.capture() + ": the " + evidence.instance().label() + " left no image at "
                    + capture);
        }
        return Optional.ofNullable(image);
    }

    private static VisualResult resultOf(int scenes, ScenarioReport report) {
        return new VisualResult(scenes, report.observations(), report.failures());
    }
}
