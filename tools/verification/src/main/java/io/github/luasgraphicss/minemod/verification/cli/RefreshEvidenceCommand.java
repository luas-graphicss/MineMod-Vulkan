package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.compatibility.evidence.EvidenceFiles;
import io.github.luasgraphicss.minemod.verification.compatibility.model.CompatibilityManifest;
import io.github.luasgraphicss.minemod.verification.compatibility.storage.CompatibilityManifestFile;
import io.github.luasgraphicss.minemod.verification.ecosystem.EcosystemScenarios;
import io.github.luasgraphicss.minemod.verification.ecosystem.evidence.ScenarioEvidence;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Set;

final class RefreshEvidenceCommand {
    private RefreshEvidenceCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of("root"));
        ScenarioEvidence evidence = new ScenarioEvidence(repositoryRoot);
        for (RepositoryScenario scenario : EcosystemScenarios.all()) {
            ScenarioReport report = evidence.run(scenario);
            if (!report.passed()) {
                output.println("scenario " + scenario.name() + " fails; its report on disk was kept");
                report.failures().forEach(failure -> output.println("  - " + failure));
                return VerificationCli.EXIT_VIOLATIONS;
            }
            evidence.write(scenario, report);
            output.println("wrote " + ScenarioEvidence.repositoryPathOf(scenario) + ": "
                    + report.observations().size() + " observation(s)");
        }
        CompatibilityManifest cases = CompatibilityManifestFile.readFrom(repositoryRoot);
        CompatibilityManifest pinned = EvidenceFiles.pinnedToFiles(repositoryRoot, cases);
        if (!pinned.equals(cases)) {
            CompatibilityManifestFile.writeTo(repositoryRoot, pinned);
            output.println("pinned the compatibility cases to the evidence on disk");
        }
        return VerificationCli.EXIT_OK;
    }
}
