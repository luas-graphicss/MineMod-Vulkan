package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.modules.AdapterModules;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.policy.TargetMatrixRules;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class BuildMatrixCommand {
    private BuildMatrixCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output) throws IOException {
        invocation.allowOnly(Set.of("manifest", "root"));
        TargetManifestFile file = TargetManifestFile.at(repositoryRoot.resolve(invocation.requiredOption("manifest")));
        TargetManifest manifest = file.read();

        List<String> violations = TargetMatrixRules.findViolations(manifest);
        if (!violations.isEmpty()) {
            output.println(file.path() + " breaks the target policy; no build was planned");
            violations.forEach(violation -> output.println("  - " + violation));
            return VerificationCli.EXIT_VIOLATIONS;
        }

        List<AdapterBuild> builds = AdapterBuildPlanner.plan(manifest);
        int readyBuilds = 0;
        for (AdapterBuild build : builds) {
            List<String> missingModules = AdapterModules.findMissing(repositoryRoot, build);
            if (missingModules.isEmpty()) {
                readyBuilds++;
            }
            output.println(describe(build, missingModules));
        }
        output.println("build matrix: " + readyBuilds + " of " + builds.size() + " combinations have their adapters");
        return readyBuilds == builds.size() ? VerificationCli.EXIT_OK : VerificationCli.EXIT_VIOLATIONS;
    }

    private static String describe(AdapterBuild build, List<String> missingModules) {
        String subject = build.release() + " " + build.loader() + ": " + build.artifactName()
                + " (Java " + build.javaRuntime() + ", " + build.toolchain() + ")";
        return missingModules.isEmpty()
                ? subject + " ready"
                : subject + " missing " + String.join(", ", missingModules);
    }
}
