package io.github.luasgraphicss.minemod.verification.targets.refresh;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import io.github.luasgraphicss.minemod.verification.targets.sources.FabricDistribution;
import io.github.luasgraphicss.minemod.verification.targets.sources.ForgeDistribution;
import io.github.luasgraphicss.minemod.verification.targets.sources.LoaderDistribution;
import io.github.luasgraphicss.minemod.verification.targets.sources.MetadataClient;
import io.github.luasgraphicss.minemod.verification.targets.sources.MojangReleaseSource;
import io.github.luasgraphicss.minemod.verification.targets.sources.NeoForgeDistribution;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class TargetManifestRefresher {
    private static final List<String> SOURCES = Stream.of(
                    MojangReleaseSource.VERSION_MANIFEST,
                    ForgeDistribution.VERSIONS,
                    ForgeDistribution.PROMOTIONS,
                    FabricDistribution.GAME_VERSIONS,
                    FabricDistribution.LOADER_VERSIONS,
                    FabricDistribution.INTERMEDIARY_VERSIONS,
                    NeoForgeDistribution.VERSIONS,
                    NeoForgeDistribution.LEGACY_VERSIONS)
            .map(URI::toString)
            .toList();

    private TargetManifestRefresher() {}

    public static TargetManifest refresh(MetadataClient client) throws IOException, InterruptedException {
        MojangReleaseSource mojang = MojangReleaseSource.load(client);
        List<LoaderDistribution> distributions = List.of(
                ForgeDistribution.load(client), FabricDistribution.load(client), NeoForgeDistribution.load(client));

        List<ReleaseTarget> releases = new ArrayList<>();
        for (RequiredRelease required : ReleaseCatalog.releases()) {
            releases.add(mojang.release(required, loadersFor(required, distributions)));
        }
        return new TargetManifest(TargetManifest.SCHEMA_VERSION, SOURCES, releases);
    }

    private static List<LoaderTarget> loadersFor(RequiredRelease required, List<LoaderDistribution> distributions) {
        List<LoaderTarget> loaders = new ArrayList<>();
        for (LoaderDistribution distribution : distributions) {
            distribution.targetFor(required).ifPresent(loaders::add);
        }
        if (required.baseLoader() == LoaderTarget.Kind.CLIENT_BOOTSTRAP) {
            loaders.add(LoaderTarget.clientBootstrap());
        }
        return loaders;
    }
}
