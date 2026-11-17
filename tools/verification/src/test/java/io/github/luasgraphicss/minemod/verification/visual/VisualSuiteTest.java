package io.github.luasgraphicss.minemod.verification.visual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.fixtures.InspectionDraft;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.JourneyRepository;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.Recordings;
import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceDirectory;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceRequest;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlanFile;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep.Kind;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VisualSuiteTest {
    private static final String BROKEN_SCENE = "underwater";

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void copyTheJourneysAndTheMod() throws IOException {
        JourneyRepository.copyInto(repositoryRoot);
        JourneyRepository.buildCandidateMod(repositoryRoot);
    }

    @Test
    void matchesWhenBothInstancesDrawTheSameScenes() throws IOException, InterruptedException {
        VisualResult result = run(capturing((instance, capture) -> Pictures.scene()));

        assertEquals(List.of(), result.differences());
        assertEquals(8, result.scenes());
        assertEquals(3 + result.scenes(), result.observations().size(), result.observations().toString());
        assertTrue(
                result.observations().getFirst().startsWith("plan vanilla-visual: world journey with seed 20240808"),
                result.observations().getFirst());
        assertEquals(
                "scene terrain-day: 128x96, mean error 0.00 of 255, 0.00% of the cells differ",
                result.observations().get(3));
    }

    @Test
    void reportsTheSceneTheCandidateDrewDifferentlyAndMarksItsCells() throws IOException, InterruptedException {
        VisualResult result = run(capturing((instance, capture) ->
                instance == GameInstance.CANDIDATE && capture.equals(BROKEN_SCENE)
                        ? Pictures.withoutTheWall(Pictures.scene())
                        : Pictures.scene()));

        Path marked = repositoryRoot.resolve(
                "cache/vanilla-journey/fabric-1.21.1/vanilla-visual/candidate/differences/underwater.png");
        assertFalse(result.matches());
        assertEquals(2, result.differences().size(), result.differences().toString());
        assertTrue(
                result.differences().get(0).startsWith("scene underwater: 8.33% of the 16-pixel cells differ"),
                result.differences().get(0));
        assertEquals("scene underwater: the cells that differ are marked in " + marked, result.differences().get(1));
        assertEquals(Pictures.WIDTH, ImageIO.read(marked.toFile()).getWidth());
    }

    @Test
    void reportsACaptureAnInstanceNeverTook() throws IOException, InterruptedException {
        VisualResult result = run(capturing((instance, capture) ->
                instance == GameInstance.REFERENCE && capture.equals(BROKEN_SCENE) ? null : Pictures.scene()));

        assertEquals(1, result.differences().size(), result.differences().toString());
        assertTrue(
                result.differences().getFirst().startsWith("scene underwater: the reference left no image at "),
                result.differences().getFirst());
    }

    @Test
    void comparesNothingWhenTheReferenceRanWithTheMod() throws IOException, InterruptedException {
        InstanceLauncher launcher = Recordings.recording(request ->
                Recordings.with(faithful(request), Recordings.CANDIDATE_MODS, null));

        VisualResult result = run(launcher);

        assertEquals(
                List.of("expected a reference that draws with the renderer of the game, without the minemod-vulkan"
                        + " mod"),
                result.differences());
    }

    private VisualResult run(InstanceLauncher launcher) throws IOException, InterruptedException {
        return new VisualSuite(launcher).run(JourneyRepository.environmentOf(repositoryRoot));
    }

    private static InstanceLauncher capturing(BiFunction<GameInstance, String, BufferedImage> frames) {
        return Recordings.recording(request -> {
            JourneyPlan plan = JourneyPlanFile.inRepository(request.repositoryRoot(), VisualSuite.NAME).read();
            for (String capture : plan.argumentsOf(Kind.CAPTURE)) {
                BufferedImage frame = frames.apply(request.instance(), capture);
                if (frame != null) {
                    Path image = InstanceDirectory.captureIn(request.directory(), capture);
                    Files.createDirectories(image.getParent());
                    ImageIO.write(frame, "png", image.toFile());
                }
            }
            return faithful(request);
        });
    }

    private static JourneyRecord faithful(InstanceRequest request) throws IOException {
        return Recordings.faithful(request, (plan, name) -> InspectionDraft.named(name).build());
    }
}
