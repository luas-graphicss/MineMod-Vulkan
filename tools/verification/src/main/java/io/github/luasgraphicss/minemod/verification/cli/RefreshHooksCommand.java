package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.policy.PresentationHookRules;
import io.github.luasgraphicss.minemod.verification.hooks.refresh.PresentationHooksRefresher;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class RefreshHooksCommand {
    private RefreshHooksCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of("root", CacheOption.NAME));
        TargetManifest targets = TargetManifestFile.inRepository(repositoryRoot).read();
        PresentationHooks hooks;
        try (PinnedDownloads downloads = new PinnedDownloads(CacheOption.directoryOf(invocation, repositoryRoot))) {
            hooks = PresentationHooksRefresher.refresh(targets, downloads);
        }

        List<String> violations = PresentationHookRules.findViolations(hooks, targets);
        if (!violations.isEmpty()) {
            output.println("the scanned clients break the hook policy; the manifest on disk was kept");
            violations.forEach(violation -> output.println("  - " + violation));
            return VerificationCli.EXIT_VIOLATIONS;
        }

        PresentationHooksFile file = PresentationHooksFile.inRepository(repositoryRoot);
        file.write(hooks);
        int callSites = hooks.releases().stream().mapToInt(ReleaseHooks::callSites).sum();
        output.println("wrote " + file.path() + ": " + hooks.releases().size() + " releases, " + callSites
                + " pinned calls");
        return VerificationCli.EXIT_OK;
    }
}
