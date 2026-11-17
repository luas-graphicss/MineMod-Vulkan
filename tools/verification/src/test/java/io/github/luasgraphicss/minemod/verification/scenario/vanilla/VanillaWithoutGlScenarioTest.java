package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.fixtures.InspectionDraft;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.JourneyRepository;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.Recordings;
import io.github.luasgraphicss.minemod.verification.journey.instance.GameInstance;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceRequest;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.PlannedDimensions;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VanillaWithoutGlScenarioTest {
    private static final List<String> WITH_OPENGL =
            List.of("libGL.so.1", "libminemod_jni.so", "libvulkan.so.1", "libvulkan_lvp.so");

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void copyTheJourneysAndTheMod() throws IOException {
        JourneyRepository.copyInto(repositoryRoot);
        JourneyRepository.buildCandidateMod(repositoryRoot);
    }

    @Test
    void passesWhenOnlyTheReferenceMapsOpenGl() throws IOException, InterruptedException {
        ScenarioReport report = run(draft -> draft, draft -> draft);

        assertEquals(List.of(), report.failures());
        assertEquals(4, report.observations().size(), report.observations().toString());
        assertTrue(
                report.observations().getFirst().startsWith("candidate: inspection title: no OpenGL library among 6"),
                report.observations().getFirst());
        assertEquals(
                "reference: inspection world: the game with its own renderer maps [libGL.so.1, libGLX.so.0]",
                report.observations().getLast());
    }

    @Test
    void failsWhenTheCandidateMapsOpenGl() throws IOException, InterruptedException {
        ScenarioReport report = run(draft -> draft.mapping(WITH_OPENGL), draft -> draft);

        assertEquals(
                List.of(
                        "candidate: inspection title: OpenGL libraries in the process: expected [], found"
                                + " [libGL.so.1]",
                        "candidate: inspection world: OpenGL libraries in the process: expected [], found"
                                + " [libGL.so.1]"),
                report.failures());
    }

    @Test
    void failsWhenDrawsOfTheGameWereLeftOut() throws IOException, InterruptedException {
        ScenarioReport report = run(draft -> draft.leavingOut(12), draft -> draft);

        assertEquals(
                List.of(
                        "candidate: inspection title: draws of the game left out of the native scene: expected 0,"
                                + " found 12",
                        "candidate: inspection world: draws of the game left out of the native scene: expected 0,"
                                + " found 12"),
                report.failures());
    }

    @Test
    void failsWhenNothingWasUploadedForTheWorld() throws IOException, InterruptedException {
        ScenarioReport report = run(draft -> draft.withBufferBytes(0), draft -> draft);

        assertEquals(
                List.of("expected candidate: inspection world: buffers uploaded to the native renderer"),
                report.failures());
    }

    @Test
    void failsWhenTheInspectionCannotSeeOpenGlInTheReference() throws IOException, InterruptedException {
        ScenarioReport report = run(draft -> draft, draft -> draft.mapping(List.of("libc.so.6")));

        assertEquals(2, report.failures().size(), report.failures().toString());
        assertTrue(
                report.failures().getFirst().startsWith("reference: inspection title: no OpenGL library was seen"),
                report.failures().getFirst());
    }

    private ScenarioReport run(UnaryOperator<InspectionDraft> candidate, UnaryOperator<InspectionDraft> reference)
            throws IOException, InterruptedException {
        return new VanillaWithoutGlScenario(Recordings.recording(request -> record(request, candidate, reference)))
                .run(JourneyRepository.environmentOf(repositoryRoot), GameTarget.reference());
    }

    private static JourneyRecord record(
            InstanceRequest request, UnaryOperator<InspectionDraft> candidate, UnaryOperator<InspectionDraft> reference)
            throws IOException {
        return Recordings.faithful(request, (plan, name) -> {
            InspectionDraft draft = InspectionDraft.named(name).in(dimensionAt(plan, name));
            return request.instance() == GameInstance.CANDIDATE
                    ? candidate.apply(draft).build()
                    : reference.apply(draft.withTheRendererOfTheGame()).build();
        });
    }

    private static String dimensionAt(JourneyPlan plan, String inspection) {
        return PlannedDimensions.atInspections(plan).get(inspection).orElse(null);
    }
}
