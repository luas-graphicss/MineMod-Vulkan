package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.refresh.AdapterModulesRefresher;
import io.github.luasgraphicss.minemod.verification.adapters.refresh.AdapterModulesRefresher.Written;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.policy.PresentationHookRules;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

final class RefreshAdaptersCommand {
    private RefreshAdaptersCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of("root", CacheOption.NAME));
        TargetManifest targets = TargetManifestFile.inRepository(repositoryRoot).read();
        PresentationHooks hooks = PresentationHooksFile.inRepository(repositoryRoot).read();
        List<String> violations = PresentationHookRules.findViolations(hooks, targets);
        if (!violations.isEmpty()) {
            output.println("the pinned hooks break their policy; no adapter was written");
            violations.forEach(violation -> output.println("  - " + violation));
            return VerificationCli.EXIT_VIOLATIONS;
        }
        Written written;
        try (NameDownloads names = new NameDownloads(CacheOption.directoryOf(invocation, repositoryRoot))) {
            written = AdapterModulesRefresher.refresh(repositoryRoot, targets, hooks, new AdapterPlanResolver(names));
        }
        output.println("wrote the plans of " + written.gameAdapters() + " game adapters and "
                + written.loaderAdapters() + " loader adapters");
        return VerificationCli.EXIT_OK;
    }
}
