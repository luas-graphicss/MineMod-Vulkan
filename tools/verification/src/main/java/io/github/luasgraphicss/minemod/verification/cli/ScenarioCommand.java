package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.scenario.TargetScenario;
import io.github.luasgraphicss.minemod.verification.scenario.TargetScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

final class ScenarioCommand {
    static final String COMMAND = "scenario";
    private static final String REPORT = "report";
    private static final String TARGET = "target";

    private ScenarioCommand() {}

    static int run(Invocation invocation, String name, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        ScenarioReport report = reportOf(invocation, name, repositoryRoot);
        report.observations().forEach(observation -> output.println("  " + observation));
        report.failures().forEach(failure -> output.println("  - " + failure));
        Optional<Path> reportFile = invocation.option(REPORT).map(repositoryRoot::resolve);
        if (reportFile.isPresent() && report.passed()) {
            Files.createDirectories(reportFile.get().getParent());
            Files.writeString(reportFile.get(), report.observationsAsText());
            output.println("wrote " + reportFile.get() + ": " + report.observations().size() + " observation(s)");
        }
        output.println(
                "scenario " + name + ": " + (report.passed() ? "passed" : report.failures().size() + " failure(s)"));
        return report.passed() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }

    private static ScenarioReport reportOf(Invocation invocation, String name, Path repositoryRoot)
            throws IOException, InterruptedException {
        Optional<Scenario> selfContained = ScenarioCatalog.named(name);
        if (selfContained.isPresent()) {
            invocation.allowOnly(Set.of("root", REPORT));
            return selfContained.get().run();
        }
        Optional<TargetScenario> targeted = TargetScenarioCatalog.named(name);
        if (targeted.isPresent()) {
            invocation.allowOnly(Set.of("root", CacheOption.NAME, REPORT, TARGET));
            GameTarget target = targetOf(invocation);
            Path cache = CacheOption.directoryOf(invocation, repositoryRoot);
            return targeted.get().run(new ScenarioEnvironment(repositoryRoot, cache), target);
        }
        RepositoryScenario scenario = RepositoryScenarioCatalog.named(name)
                .orElseThrow(() -> new UsageException("unknown scenario: " + name));
        invocation.allowOnly(Set.of("root", CacheOption.NAME, REPORT));
        Path cache = CacheOption.directoryOf(invocation, repositoryRoot);
        return scenario.run(new ScenarioEnvironment(repositoryRoot, cache));
    }

    private static GameTarget targetOf(Invocation invocation) {
        try {
            return GameTarget.parse(invocation.requiredOption(TARGET));
        } catch (IllegalArgumentException malformed) {
            throw new UsageException(malformed.getMessage());
        }
    }
}
