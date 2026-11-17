package io.github.luasgraphicss.minemod.verification.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.ecosystem.evidence.ScenarioEvidence;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EcosystemScenariosTest {
    private static final Path REPOSITORY_ROOT = Path.of(System.getProperty("minemod.repositoryRoot"));

    @Test
    void everyScenarioPassesAndMatchesItsCommittedReport() throws IOException {
        assertEquals(List.of(), new EcosystemScenariosCheck().findViolations(REPOSITORY_ROOT));
    }

    @Test
    void theScenariosAreFoundByTheNamesTheCommandLineTakes() {
        assertEquals(
                List.of("modpack-matrix", "renderer-conflicts", "shader-integration"),
                EcosystemScenarios.all().stream().map(RepositoryScenario::name).toList());
        assertTrue(RepositoryScenarioCatalog.named("modpack-matrix").isPresent());
        assertTrue(RepositoryScenarioCatalog.named("production-transformers").isPresent());
    }

    @Test
    void aReportThatChangedNoLongerMatchesTheEvidence(@TempDir Path repository) throws IOException {
        RepositoryScenario scenario = EcosystemScenarios.all().get(0);
        ScenarioEvidence evidence = new ScenarioEvidence(repository);

        assertFalse(evidence.matches(scenario, new ScenarioReport(List.of("ran 21 packs"), List.of())));
        evidence.write(scenario, new ScenarioReport(List.of("ran 21 packs"), List.of()));

        assertTrue(evidence.matches(scenario, new ScenarioReport(List.of("ran 21 packs"), List.of())));
        assertFalse(evidence.matches(scenario, new ScenarioReport(List.of("ran 20 packs"), List.of())));
        assertEquals("docs/compatibility/reports/modpack-matrix.txt", ScenarioEvidence.repositoryPathOf(scenario));
    }
}
