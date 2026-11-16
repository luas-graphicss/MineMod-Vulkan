package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class FabricDistribution implements LoaderDistribution {
    public static final URI GAME_VERSIONS = URI.create("https://meta.fabricmc.net/v2/versions/game");
    public static final URI LOADER_VERSIONS = URI.create("https://meta.fabricmc.net/v2/versions/loader");
    public static final URI INTERMEDIARY_VERSIONS = URI.create("https://meta.fabricmc.net/v2/versions/intermediary");

    private final Set<String> gameVersions;
    private final String loaderArtifact;
    private final Map<String, String> intermediaryByGameVersion;

    private FabricDistribution(
            Set<String> gameVersions, String loaderArtifact, Map<String, String> intermediaryByGameVersion) {
        this.gameVersions = gameVersions;
        this.loaderArtifact = loaderArtifact;
        this.intermediaryByGameVersion = intermediaryByGameVersion;
    }

    public static FabricDistribution load(MetadataClient client) throws IOException, InterruptedException {
        return fromMetadata(
                client.fetch(GAME_VERSIONS), client.fetch(LOADER_VERSIONS), client.fetch(INTERMEDIARY_VERSIONS));
    }

    static FabricDistribution fromMetadata(JsonNode games, JsonNode loaders, JsonNode intermediaries)
            throws MetadataFormatException {
        Set<String> gameVersions = new HashSet<>();
        for (JsonNode game : games) {
            gameVersions.add(JsonFields.text(game, "version"));
        }
        Map<String, String> intermediaryByGameVersion = new HashMap<>();
        for (JsonNode intermediary : intermediaries) {
            intermediaryByGameVersion.put(
                    JsonFields.text(intermediary, "version"), JsonFields.text(intermediary, "maven"));
        }
        return new FabricDistribution(gameVersions, newestStableLoader(loaders), intermediaryByGameVersion);
    }

    @Override
    public Optional<LoaderTarget> targetFor(RequiredRelease release) {
        String mappings = intermediaryByGameVersion.get(release.id());
        if (mappings == null || !gameVersions.contains(release.id())) {
            return Optional.empty();
        }
        return Optional.of(new LoaderTarget(
                LoaderTarget.Kind.FABRIC,
                loaderArtifact,
                LoaderTarget.Channel.STABLE,
                LoaderTarget.Bootstrap.FABRIC_KNOT,
                mappings,
                LoaderTarget.Toolchain.FABRIC_LOOM));
    }

    private static String newestStableLoader(JsonNode loaders) throws MetadataFormatException {
        for (JsonNode loader : loaders) {
            if (JsonFields.require(loader, "stable").asBoolean()) {
                return JsonFields.text(loader, "maven");
            }
        }
        throw new MetadataFormatException("Fabric metadata lists no stable loader");
    }
}
