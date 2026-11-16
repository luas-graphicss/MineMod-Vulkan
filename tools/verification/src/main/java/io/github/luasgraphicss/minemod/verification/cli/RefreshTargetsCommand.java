package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.policy.TargetMatrixRules;
import io.github.luasgraphicss.minemod.verification.targets.refresh.TargetManifestRefresher;
import io.github.luasgraphicss.minemod.verification.targets.sources.MetadataClient;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class RefreshTargetsCommand {
    private RefreshTargetsCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of("root"));
        TargetManifest manifest;
        try (MetadataClient client = new MetadataClient()) {
            manifest = TargetManifestRefresher.refresh(client);
        }

        List<String> violations = TargetMatrixRules.findViolations(manifest);
        if (!violations.isEmpty()) {
            output.println("refreshed metadata breaks the target policy; the manifest on disk was kept");
            violations.forEach(violation -> output.println("  - " + violation));
            return VerificationCli.EXIT_VIOLATIONS;
        }

        TargetManifestFile file = TargetManifestFile.inRepository(repositoryRoot);
        file.write(manifest);
        long combinations = manifest.releases().stream().mapToLong(release -> release.loaders().size()).sum();
        output.println("wrote " + file.path() + ": " + manifest.releases().size() + " releases, "
                + combinations + " loader combinations");
        return VerificationCli.EXIT_OK;
    }
}
