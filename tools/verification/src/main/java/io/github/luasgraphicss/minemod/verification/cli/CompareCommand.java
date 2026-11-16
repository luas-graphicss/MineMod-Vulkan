package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.differential.DifferentialResult;
import io.github.luasgraphicss.minemod.verification.differential.DifferentialSuite;
import java.io.PrintStream;
import java.util.Set;

final class CompareCommand {
    private CompareCommand() {}

    static int run(Invocation invocation, PrintStream output) {
        invocation.allowOnly(Set.of("suite", "root"));
        String suite = invocation.requiredOption("suite");
        if (!suite.equals(DifferentialSuite.NAME)) {
            throw new UsageException("unknown suite: " + suite);
        }
        DifferentialResult result = DifferentialSuite.run();
        result.differences().forEach(difference -> output.println("  - " + difference));
        output.println("suite " + suite + ": " + result.scripts() + " scripts against the "
                + DifferentialSuite.PROVIDER + " provider, " + result.differences().size() + " difference(s)");
        return result.differences().isEmpty() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }
}
