package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.check.CheckProfile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class CheckCommand {
    private CheckCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output) throws IOException {
        invocation.allowOnly(Set.of("profile", "root"));
        String profileName = invocation.requiredOption("profile");
        CheckProfile profile = CheckProfile.named(profileName)
                .orElseThrow(() -> new UsageException("unknown profile: " + profileName));

        int failedChecks = 0;
        for (Check check : profile.checks()) {
            List<String> violations = check.findViolations(repositoryRoot);
            if (violations.isEmpty()) {
                output.println("check " + check.name() + ": ok");
                continue;
            }
            failedChecks++;
            output.println("check " + check.name() + ": " + violations.size() + " violation(s)");
            violations.forEach(violation -> output.println("  - " + violation));
        }

        int passedChecks = profile.checks().size() - failedChecks;
        output.println("profile " + profile.name() + ": " + passedChecks + " passed, " + failedChecks + " failed");
        return failedChecks == 0 ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }
}
