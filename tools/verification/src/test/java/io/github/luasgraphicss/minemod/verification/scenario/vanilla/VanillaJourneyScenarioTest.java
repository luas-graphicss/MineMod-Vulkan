package io.github.luasgraphicss.minemod.verification.scenario.vanilla;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.fixtures.InspectionDraft;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.JourneyRepository;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.Recordings;
import io.github.luasgraphicss.minemod.verification.journey.instance.InstanceLauncher;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan;
import io.github.luasgraphicss.minemod.verification.journey.plan.PlannedDimensions;
import io.github.luasgraphicss.minemod.verification.journey.record.Inspection;
import io.github.luasgraphicss.minemod.verification.journey.record.JourneyRecord.Status;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VanillaJourneyScenarioTest {
    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void copyTheJourneys() throws IOException {
        JourneyRepository.copyInto(repositoryRoot);
    }

    @Test
    void passesWhenEveryLegRanOnTheNativeRenderer() throws IOException, InterruptedException {
        JourneyRepository.buildCandidateMod(repositoryRoot);

        ScenarioReport report = run(Recordings.recording(
                request -> Recordings.faithful(request, VanillaJourneyScenarioTest::asPlanned)));

        assertEquals(List.of(), report.failures());
        assertEquals(7, report.observations().size(), report.observations().toString());
        assertTrue(
                report.observations().getFirst().startsWith("inspection overworld: minecraft:overworld, 120 frame(s)"),
                report.observations().getFirst());
        assertTrue(
                report.observations().getLast().contains("[minecraft:overworld, minecraft:the_end, minecraft:the_nether]"),
                report.observations().getLast());
    }

    @Test
    void asksForTheModBeforeStartingTheGame() throws IOException, InterruptedException {
        ScenarioReport report = run(request -> {
            throw new AssertionError("no instance starts without the mod");
        });

        assertEquals(
                List.of("adapters/fabric/mc-1.21.1 has no mod jar; build it with"
                        + " \"sh gradlew -p adapters/fabric/mc-1.21.1 build\""),
                report.failures());
    }

    @Test
    void refusesATargetWithoutAJourneyDriver() throws IOException, InterruptedException {
        ScenarioReport report = new VanillaJourneyScenario(request -> {
                    throw new AssertionError("no instance starts without a driver");
                })
                .run(JourneyRepository.environmentOf(repositoryRoot), GameTarget.parse("forge-1.20.1"));

        assertEquals(
                List.of("forge-1.20.1 has no journey driver under tests/integration/vanilla-journey"),
                report.failures());
    }

    @Test
    void failsOnTheStepThatDidNotFinish() throws IOException, InterruptedException {
        JourneyRepository.buildCandidateMod(repositoryRoot);

        ScenarioReport report = run(Recordings.recording(request -> Recordings.stoppedAt(
                Recordings.faithful(request, VanillaJourneyScenarioTest::asPlanned),
                1,
                Status.TIMED_OUT,
                "not done after 300 s")));

        assertEquals("step 1 (create-world) timed-out: not done after 300 s", report.failures().getFirst());
        assertTrue(report.failures().contains("step 2 (command gamerule doDaylightCycle false) never ran"));
    }

    @Test
    void failsWhenTheGameDoesNotLeaveCleanly() throws IOException, InterruptedException {
        JourneyRepository.buildCandidateMod(repositoryRoot);

        ScenarioReport report = run(Recordings.endingWith(
                134, request -> Recordings.faithful(request, VanillaJourneyScenarioTest::asPlanned)));

        assertEquals(1, report.failures().size(), report.failures().toString());
        assertTrue(
                report.failures().getFirst().startsWith("the instance ended with exit code 134; see "),
                report.failures().getFirst());
    }

    @Test
    void failsWhenTheWorldIsNotDrawnWhereThePlanWent() throws IOException, InterruptedException {
        JourneyRepository.buildCandidateMod(repositoryRoot);

        ScenarioReport report = run(Recordings.recording(request -> Recordings.faithful(request, (plan, name) -> {
            InspectionDraft draft = InspectionDraft.named(name).in(dimensionAt(plan, name));
            return name.equals("nether")
                    ? draft.in("minecraft:overworld").withWorldDraws(0).build()
                    : draft.build();
        })));

        assertEquals(
                List.of(
                        "inspection nether: dimension: expected minecraft:the_nether, found minecraft:overworld",
                        "expected inspection nether: draws in the world pass of the native renderer",
                        "expected inspections in [minecraft:overworld, minecraft:the_end, minecraft:the_nether], found"
                                + " [minecraft:overworld, minecraft:the_end]"),
                report.failures());
    }

    @Test
    void failsWhenTheCompatibilityProviderOrTheGameRendererTookPart() throws IOException, InterruptedException {
        JourneyRepository.buildCandidateMod(repositoryRoot);

        ScenarioReport report = run(Recordings.recording(request -> Recordings.with(
                Recordings.faithful(request, (plan, name) -> InspectionDraft.named(name)
                        .in(dimensionAt(plan, name))
                        .withTheRendererOfTheGame()
                        .build()),
                Recordings.REFERENCE_MODS,
                "/opt/provider/manifest.json")));

        assertTrue(
                report.failures().contains("the instance ran with the compatibility provider /opt/provider/manifest.json"),
                report.failures().toString());
        assertTrue(
                report.failures().contains("inspection overworld: the native renderer reported nothing"),
                report.failures().toString());
        assertTrue(report.failures().getFirst().startsWith("the instance ran without the minemod-vulkan mod"));
    }

    private ScenarioReport run(InstanceLauncher launcher) throws IOException, InterruptedException {
        return new VanillaJourneyScenario(launcher)
                .run(JourneyRepository.environmentOf(repositoryRoot), GameTarget.reference());
    }

    private static Inspection asPlanned(JourneyPlan plan, String name) {
        return InspectionDraft.named(name).in(dimensionAt(plan, name)).build();
    }

    private static String dimensionAt(JourneyPlan plan, String inspection) {
        return PlannedDimensions.atInspections(plan).get(inspection).orElse(null);
    }
}
