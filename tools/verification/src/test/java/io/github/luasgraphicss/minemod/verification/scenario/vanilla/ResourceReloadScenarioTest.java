package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.fixtures.InspectionDraft;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.JourneyRepository;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.Recordings;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceReloadScenarioTest {
    private static final String REFUSAL = "position_tex_color.fsh:11: syntax error";

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void copyTheJourneysAndTheMod() throws IOException {
        JourneyRepository.copyInto(repositoryRoot);
        JourneyRepository.buildCandidateMod(repositoryRoot);
    }

    @Test
    void passesWhenTheRefusedReloadKeepsTheGeneration() throws IOException, InterruptedException {
        ScenarioReport report = run(Map.of());

        assertEquals(List.of(), report.failures());
        assertEquals(
                List.of(
                        "inspection baseline: generation 1, built-in programs, 0 resource(s) retiring",
                        "inspection repeated: generation 1, built-in programs, 0 resource(s) retiring",
                        "inspection replaced: generation 2, 1 program(s) replaced by resource packs, 0 resource(s)"
                                + " retiring",
                        "inspection refused: generation 2, reload refused (" + REFUSAL + "), 0 resource(s) retiring",
                        "inspection restored: generation 3, built-in programs, 0 resource(s) retiring",
                        "the game reloaded its resources 6 time(s); the refused reload kept generation 2 and frames"
                                + " kept being measured after it"),
                report.observations());
    }

    @Test
    void failsWhenTheRefusedReloadPublishedAGeneration() throws IOException, InterruptedException {
        ScenarioReport report = run(Map.of("refused", draft -> draft.atGeneration(3)));

        assertEquals(
                List.of("generation kept by the reload the renderer refused: expected 2, found 3"),
                report.failures());
    }

    @Test
    void failsWhenTheBrokenPackWasPublished() throws IOException, InterruptedException {
        ScenarioReport report = run(Map.of("refused", draft -> draft.withShaders(InspectionDraft.ONE_REPLACED)));

        assertEquals(
                List.of("inspection refused: programs: expected REFUSED, found REPLACED"), report.failures());
    }

    @Test
    void failsWhenRetiredResourcesNeverLeave() throws IOException, InterruptedException {
        ScenarioReport report = run(Map.of("restored", draft -> draft.retiring(5)));

        assertEquals(
                List.of("resources still retiring once the last reload settled: expected 0, found 5"),
                report.failures());
    }

    @Test
    void failsWhenFramesStopAfterTheRefusedReload() throws IOException, InterruptedException {
        ScenarioReport report = run(Map.of("refused", draft -> draft.withFrames(0)));

        assertEquals(
                List.of("expected inspection refused: frames measured by the native renderer"), report.failures());
    }

    @Test
    void theJourneySelectsTheTwoPacksAndGoesBackToNone() throws IOException, InterruptedException {
        ScenarioReport report = run(Map.of());

        assertTrue(report.passed());
        assertTrue(Files.isRegularFile(repositoryRoot.resolve(
                "cache/vanilla-journey/fabric-1.21.1/resource-reload/candidate/game/resourcepacks/"
                        + "broken-core-shader/pack.mcmeta")));
    }

    private ScenarioReport run(Map<String, UnaryOperator<InspectionDraft>> changes)
            throws IOException, InterruptedException {
        Map<String, InspectionDraft> drafts = new HashMap<>();
        drafts.put("baseline", InspectionDraft.named("baseline"));
        drafts.put("repeated", InspectionDraft.named("repeated"));
        drafts.put(
                "replaced",
                InspectionDraft.named("replaced").atGeneration(2).withShaders(InspectionDraft.ONE_REPLACED));
        drafts.put("refused", InspectionDraft.named("refused").atGeneration(2).refusing(REFUSAL));
        drafts.put("restored", InspectionDraft.named("restored").atGeneration(3));
        changes.forEach((name, change) -> drafts.compute(name, (inspection, draft) -> change.apply(draft)));
        return new ResourceReloadScenario(Recordings.recording(request ->
                        Recordings.faithful(request, (plan, name) -> drafts.get(name).build())))
                .run(JourneyRepository.environmentOf(repositoryRoot), GameTarget.reference());
    }
}
