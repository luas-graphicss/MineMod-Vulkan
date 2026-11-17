package io.github.luasgraphicss.minemod.verification.hooks.refresh;

import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.LibraryCall;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.discovery.HookDiscovery;
import io.github.luasgraphicss.minemod.verification.hooks.mappings.MojangNames;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks.Windowing;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSite;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PresentationHooksRefresher {
    private PresentationHooksRefresher() {}

    public static PresentationHooks refresh(TargetManifest targets, PinnedDownloads downloads)
            throws IOException, InterruptedException {
        List<ReleaseHooks> releases = new ArrayList<>();
        for (ReleaseTarget release : targets.releases()) {
            releases.add(hooksOf(release, downloads));
        }
        return new PresentationHooks(PresentationHooks.SCHEMA_VERSION, releases);
    }

    private static ReleaseHooks hooksOf(ReleaseTarget release, PinnedDownloads downloads)
            throws IOException, InterruptedException {
        Windowing windowing = Windowing.ofLwjglGeneration(release.lwjgl().generation());
        List<LibraryCall> calls = PresentationCalls.of(windowing);
        Function<CallSite, String> namedMethodOf = namedMethodsOf(downloads.clientMappingsOf(release));
        try (ClientArchive archive = ClientArchive.open(downloads.clientOf(release))) {
            List<CallSite> sites = new CallSiteScanner(calls).scan(archive);
            return new ReleaseHooks(
                    release.id(),
                    release.client().sha1(),
                    windowing,
                    HookDiscovery.pointsOf(calls, sites, namedMethodOf));
        }
    }

    private static Function<CallSite, String> namedMethodsOf(Optional<Path> mappings) throws IOException {
        if (mappings.isEmpty()) {
            return site -> null;
        }
        MojangNames names = MojangNames.read(mappings.get());
        return site -> names.namedMethodOf(site.owner(), site.method(), site.descriptor()).orElse(null);
    }
}
