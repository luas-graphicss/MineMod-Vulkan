package io.github.luasgraphicss.minemod.verification.cli;

import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.frame.refresh.FramePlansRefresher;
import io.github.luasgraphicss.minemod.verification.frame.refresh.FramePlansRefresher.Written;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Set;

final class RefreshFramesCommand {
    private RefreshFramesCommand() {}

    static int run(Invocation invocation, Path repositoryRoot, PrintStream output)
            throws IOException, InterruptedException {
        invocation.allowOnly(Set.of("root", CacheOption.NAME));
        TargetManifest targets = TargetManifestFile.inRepository(repositoryRoot).read();
        Path cache = CacheOption.directoryOf(invocation, repositoryRoot);
        Written written;
        try (PinnedDownloads clients = new PinnedDownloads(cache);
                NameDownloads names = new NameDownloads(cache)) {
            written = FramePlansRefresher.refresh(repositoryRoot, targets, clients, new AdapterPlanResolver(names));
        }
        output.println("wrote " + written.plans() + " frame plans that hand " + written.pinnedCalls()
                + " OpenGL calls of the game to their stand-ins");
        return VerificationCli.EXIT_OK;
    }
}
