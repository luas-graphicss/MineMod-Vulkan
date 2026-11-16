package io.github.luasgraphicss.minemod.verification.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioCatalogTest {
    @Test
    void everyScenarioPassesAndSaysWhatItRan() {
        for (Scenario scenario : ScenarioCatalog.all()) {
            ScenarioReport report = scenario.run();

            assertEquals(List.of(), report.failures(), scenario.name());
            assertFalse(report.observations().isEmpty(), scenario.name());
        }
    }

    @Test
    void theScenariosOfTheCompatibilityLayerAreFoundByName() {
        assertEquals(
                List.of(
                        "mixed-render-state",
                        "legacy-gl-contract",
                        "shared-resource-lifecycle",
                        "synchronous-operations"),
                ScenarioCatalog.all().stream().map(Scenario::name).toList());
        assertTrue(ScenarioCatalog.named("legacy-gl-contract").isPresent());
        assertTrue(ScenarioCatalog.named("imaginary").isEmpty());
    }

    @Test
    void aScenarioLogKeepsFailedExpectationsApartFromObservations() {
        ScenarioLog log = new ScenarioLog();

        log.observe("ran 3 frames");
        log.expect(true, "the frame to be presented");
        log.expect(false, "no flush");
        log.expectEqual(2, 3, "handovers");

        ScenarioReport report = log.report();
        assertFalse(report.passed());
        assertEquals(List.of("ran 3 frames"), report.observations());
        assertEquals(List.of("expected no flush", "handovers: expected 2, found 3"), report.failures());
    }
}
