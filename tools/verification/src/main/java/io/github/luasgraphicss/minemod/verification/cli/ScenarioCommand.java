package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

final class ScenarioCommand {
    static final String COMMAND = "scenario";

    private ScenarioCommand() {}

    static int run(Invocation invocation, String name, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        Optional<Scenario> selfContained = ScenarioCatalog.named(name);
        if (selfContained.isPresent()) {
            invocation.allowOnly(Set.of("root"));
            return print(name, selfContained.get().run(), output);
        }
        RepositoryScenario scenario = RepositoryScenarioCatalog.named(name)
                .orElseThrow(() -> new UsageException("unknown scenario: " + name));
        invocation.allowOnly(Set.of("root", CacheOption.NAME));
        Path cache = CacheOption.directoryOf(invocation, repositoryRoot);
        return print(name, scenario.run(new ScenarioEnvironment(repositoryRoot, cache)), output);
    }

    private static int print(String name, ScenarioReport report, PrintStream output) {
        report.observations().forEach(observation -> output.println("  " + observation));
        report.failures().forEach(failure -> output.println("  - " + failure));
        output.println(
                "scenario " + name + ": " + (report.passed() ? "passed" : report.failures().size() + " failure(s)"));
        return report.passed() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }
}
