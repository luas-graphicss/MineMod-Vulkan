package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import java.io.PrintStream;
import java.util.Set;

final class ScenarioCommand {
    static final String COMMAND = "scenario";

    private ScenarioCommand() {}

    static int run(Invocation invocation, String name, PrintStream output) {
        invocation.allowOnly(Set.of("root"));
        Scenario scenario = ScenarioCatalog.named(name)
                .orElseThrow(() -> new UsageException("unknown scenario: " + name));
        ScenarioReport report = scenario.run();
        report.observations().forEach(observation -> output.println("  " + observation));
        report.failures().forEach(failure -> output.println("  - " + failure));
        output.println("scenario " + scenario.name() + ": "
                + (report.passed() ? "passed" : report.failures().size() + " failure(s)"));
        return report.passed() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }
}
