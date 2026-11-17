package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.differential.DifferentialResult;
import io.github.luasgraphicss.minemod.verification.differential.DifferentialSuite;
import io.github.luasgraphicss.minemod.verification.differential.provider.SystemProvider;
import io.github.luasgraphicss.minemod.verification.differential.simulated.SimulatedComparison;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

final class CompareCommand {
    private static final String PROVIDER = "provider";
    private static final String NATIVES = "natives";

    private CompareCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output) throws IOException {
        invocation.allowOnly(Set.of("suite", PROVIDER, NATIVES, "root"));
        String suite = invocation.requiredOption("suite");
        if (!suite.equals(DifferentialSuite.NAME)) {
            throw new UsageException("unknown suite: " + suite);
        }
        DifferentialResult result = differential(invocation, repositoryRoot);
        result.differences().forEach(difference -> output.println("  - " + difference));
        output.println("suite " + suite + ": " + result.scripts() + " scripts against " + result.provider() + ", "
                + result.differences().size() + " difference(s)");
        return result.differences().isEmpty() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }

    private static DifferentialResult differential(Invocation invocation, Path repositoryRoot) throws IOException {
        Optional<String> provider = invocation.option(PROVIDER);
        Optional<String> natives = invocation.option(NATIVES);
        if (provider.isEmpty() && natives.isEmpty()) {
            return DifferentialSuite.run(SimulatedComparison::ofProfile);
        }
        if (provider.isEmpty() || natives.isEmpty()) {
            throw new UsageException("--" + PROVIDER + " and --" + NATIVES + " name a provider together");
        }
        try (SystemProvider staged = SystemProvider.stage(
                repositoryRoot.resolve(provider.get()), repositoryRoot.resolve(natives.get()))) {
            return DifferentialSuite.run(staged);
        }
    }
}
