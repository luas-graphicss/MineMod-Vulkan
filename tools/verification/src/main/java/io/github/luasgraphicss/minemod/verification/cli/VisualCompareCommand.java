package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.journey.instance.GradleInstanceLauncher;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.visual.VisualResult;
import io.github.luasgraphicss.minemod.verification.visual.VisualSuite;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

final class VisualCompareCommand {
    private static final String REPORT = "report";

    private VisualCompareCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of("suite", "root", CacheOption.NAME, REPORT));
        Path cache = CacheOption.directoryOf(invocation, repositoryRoot);
        VisualResult result =
                new VisualSuite(new GradleInstanceLauncher()).run(new ScenarioEnvironment(repositoryRoot, cache));
        result.observations().forEach(observation -> output.println("  " + observation));
        result.differences().forEach(difference -> output.println("  - " + difference));
        Optional<Path> reportFile = invocation.option(REPORT).map(repositoryRoot::resolve);
        if (reportFile.isPresent() && result.matches()) {
            Files.createDirectories(reportFile.get().getParent());
            Files.writeString(reportFile.get(), result.observationsAsText());
            output.println("wrote " + reportFile.get() + ": " + result.observations().size() + " observation(s)");
        }
        output.println("suite " + VisualSuite.NAME + ": " + result.scenes()
                + " scene(s) of the game with its own renderer against the native one, "
                + result.differences().size() + " difference(s)");
        return result.matches() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }
}
