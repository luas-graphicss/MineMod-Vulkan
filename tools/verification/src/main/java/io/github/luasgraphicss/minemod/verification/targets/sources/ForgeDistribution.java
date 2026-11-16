package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ForgeDistribution implements LoaderDistribution {
    public static final URI VERSIONS =
            URI.create("https://files.minecraftforge.net/net/minecraftforge/forge/maven-metadata.json");
    public static final URI PROMOTIONS =
            URI.create("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json");

    private static final String ARTIFACT_PREFIX = "net.minecraftforge:forge:";
    private static final List<Map.Entry<String, LoaderTarget.Channel>> PROMOTION_ORDER = List.of(
            Map.entry("recommended", LoaderTarget.Channel.RECOMMENDED),
            Map.entry("latest", LoaderTarget.Channel.LATEST));

    private final JsonNode versionsByRelease;
    private final JsonNode promotions;

    private ForgeDistribution(JsonNode versionsByRelease, JsonNode promotions) {
        this.versionsByRelease = versionsByRelease;
        this.promotions = promotions;
    }

    public static ForgeDistribution load(MetadataClient client) throws IOException, InterruptedException {
        return fromMetadata(client.fetch(VERSIONS), client.fetch(PROMOTIONS));
    }

    static ForgeDistribution fromMetadata(JsonNode versionsByRelease, JsonNode promotions)
            throws MetadataFormatException {
        return new ForgeDistribution(versionsByRelease, JsonFields.require(promotions, "promos"));
    }

    @Override
    public Optional<LoaderTarget> targetFor(RequiredRelease release) {
        List<String> published = new ArrayList<>();
        versionsByRelease.path(release.id()).forEach(version -> published.add(version.asText()));
        if (published.isEmpty()) {
            return Optional.empty();
        }
        for (Map.Entry<String, LoaderTarget.Channel> promotion : PROMOTION_ORDER) {
            String promoted = release.id() + "-" + promotions.path(release.id() + "-" + promotion.getKey()).asText();
            if (published.contains(promoted)) {
                return Optional.of(target(release, promoted, promotion.getValue()));
            }
        }
        return Optional.of(target(release, published.get(published.size() - 1), LoaderTarget.Channel.LATEST));
    }

    private static LoaderTarget target(RequiredRelease release, String version, LoaderTarget.Channel channel) {
        return new LoaderTarget(
                LoaderTarget.Kind.FORGE,
                ARTIFACT_PREFIX + version,
                channel,
                release.forgeBootstrap(),
                null,
                LoaderTarget.Toolchain.FORGEGRADLE);
    }
}
