package io.github.luasgraphicss.minemod.verification.journey.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.fixtures.JourneyRepository;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyPlan.Scene;
import io.github.luasgraphicss.minemod.verification.journey.plan.JourneyStep.Kind;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JourneyPlanRulesTest {
    private static final List<String> COMMITTED =
            List.of("vanilla-journey", "vanilla-without-gl", "resource-reload", "vanilla-visual");
    private static final Map<String, String> VERSIONED_OPTIONS = Map.of("version", "3955");

    @Test
    void theCommittedPlansFollowTheRules() throws IOException {
        for (String name : COMMITTED) {
            JourneyPlan plan = JourneyPlanFile.inRepository(JourneyRepository.committed(), name).read();

            assertEquals(name, plan.name());
            assertEquals(List.of(), JourneyPlanRules.findViolations(plan), name);
        }
    }

    @Test
    void theVisualPlanComparesEveryCaptureItTakes() throws IOException {
        JourneyPlan plan = JourneyPlanFile.inRepository(JourneyRepository.committed(), "vanilla-visual").read();

        assertEquals(
                plan.argumentsOf(Kind.CAPTURE), plan.scenes().stream().map(Scene::capture).toList());
    }

    @Test
    void aPlanStartsAtTheTitleAndEndsByQuitting() {
        JourneyPlan plan = planOf(List.of(new JourneyStep(Kind.CREATE_WORLD, "")), List.of());

        assertEquals(
                List.of(
                        "the journey starts by waiting for the title screen",
                        "the journey ends by quitting the game"),
                JourneyPlanRules.findViolations(plan));
    }

    @Test
    void theOptionsNameTheVersionOfTheirFile() {
        JourneyPlan plan = planOf(Map.of("fov", "0.0"), journey(), List.of());

        assertEquals(
                List.of("the options do not name the version of their file, so the game would upgrade their values"),
                JourneyPlanRules.findViolations(plan));
    }

    @Test
    void namesStayInsideTheInstanceDirectoryAndAreNotRepeated() {
        JourneyPlan plan = planOf(
                journey(
                        new JourneyStep(Kind.CAPTURE, "../outside"),
                        new JourneyStep(Kind.INSPECT, "world"),
                        new JourneyStep(Kind.INSPECT, "world")),
                List.of());

        List<String> violations = JourneyPlanRules.findViolations(plan);

        assertEquals(2, violations.size(), violations.toString());
        assertTrue(violations.get(0).startsWith("step 1 (capture ../outside) has a name that"), violations.get(0));
        assertEquals("step 3 (inspect world) repeats a name", violations.get(1));
    }

    @Test
    void aPlanSelectsOnlyThePacksItInstalls() {
        JourneyPlan plan = planOf(journey(new JourneyStep(Kind.SELECT_PACKS, "file/absent")), List.of());

        assertEquals(
                List.of("step 1 (select-packs file/absent) selects packs the plan does not install"),
                JourneyPlanRules.findViolations(plan));
    }

    @Test
    void aSceneComparesACaptureOfTheJourney() {
        JourneyPlan plan = planOf(journey(), List.of(new Scene("missing", 16, 32, 0.03, 6.0)));

        assertEquals(
                List.of("scene missing compares a capture the journey never takes"),
                JourneyPlanRules.findViolations(plan));
    }

    @Test
    void theDimensionOfAnInspectionFollowsTheJourney() {
        JourneyPlan plan = planOf(
                journey(
                        new JourneyStep(Kind.INSPECT, "title"),
                        new JourneyStep(Kind.CREATE_WORLD, ""),
                        new JourneyStep(Kind.INSPECT, "created"),
                        new JourneyStep(Kind.EXPECT_DIMENSION, "minecraft:the_end"),
                        new JourneyStep(Kind.INSPECT, "end"),
                        new JourneyStep(Kind.LEAVE_WORLD, ""),
                        new JourneyStep(Kind.INSPECT, "left"),
                        new JourneyStep(Kind.OPEN_WORLD, ""),
                        new JourneyStep(Kind.INSPECT, "reopened")),
                List.of());

        assertEquals(
                Map.of(
                        "title", Optional.empty(),
                        "created", Optional.of("minecraft:overworld"),
                        "end", Optional.of("minecraft:the_end"),
                        "left", Optional.empty(),
                        "reopened", Optional.of("minecraft:the_end")),
                PlannedDimensions.atInspections(plan));
    }

    private static List<JourneyStep> journey(JourneyStep... between) {
        List<JourneyStep> steps = new ArrayList<>();
        steps.add(new JourneyStep(Kind.WAIT_FOR_TITLE, ""));
        steps.addAll(List.of(between));
        steps.add(new JourneyStep(Kind.QUIT, ""));
        return steps;
    }

    private static JourneyPlan planOf(List<JourneyStep> steps, List<Scene> scenes) {
        return planOf(VERSIONED_OPTIONS, steps, scenes);
    }

    private static JourneyPlan planOf(Map<String, String> options, List<JourneyStep> steps, List<Scene> scenes) {
        return new JourneyPlan(
                "drafted", 30, 300, new JourneyPlan.World("journey", 1), options, List.of(), steps, scenes);
    }
}
