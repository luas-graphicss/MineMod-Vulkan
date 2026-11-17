package io.github.luasgraphicss.minemod.verification.ecosystem;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.ecosystem.evidence.ScenarioEvidence;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EcosystemScenariosCheck implements Check {
    @Override
    public String name() {
        return "ecosystem-scenarios";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        ScenarioEvidence evidence = new ScenarioEvidence(repositoryRoot);
        List<String> violations = new ArrayList<>();
        for (RepositoryScenario scenario : EcosystemScenarios.all()) {
            ScenarioReport report = run(scenario, evidence);
            report.failures().forEach(failure -> violations.add(scenario.name() + ": " + failure));
            if (report.passed() && !evidence.matches(scenario, report)) {
                violations.add(ScenarioEvidence.repositoryPathOf(scenario) + " is not what scenario " + scenario.name()
                        + " reports now; rewrite it with \"evidence refresh\"");
            }
        }
        return violations;
    }

    private static ScenarioReport run(RepositoryScenario scenario, ScenarioEvidence evidence) throws IOException {
        try {
            return evidence.run(scenario);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("scenario " + scenario.name() + " was interrupted", interrupted);
        }
    }
}
