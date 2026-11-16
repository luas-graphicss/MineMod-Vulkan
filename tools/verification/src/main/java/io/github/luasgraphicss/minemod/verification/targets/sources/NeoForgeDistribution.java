package io.github.luasgraphicss.minemod.verification.targets.sources;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NeoForgeDistribution implements LoaderDistribution {
    public static final URI VERSIONS =
            URI.create("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge");
    public static final URI LEGACY_VERSIONS =
            URI.create("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/forge");

    private static final String ARTIFACT_PREFIX = "net.neoforged:neoforge:";
    private static final String LEGACY_ARTIFACT_PREFIX = "net.neoforged:forge:";
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-beta)?");
    private static final Pattern LEGACY_VERSION = Pattern.compile("(\\d+\\.\\d+(?:\\.\\d+)?)-(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final int RANK_RADIX = 100_000;

    private record Candidate(String artifact, long rank, boolean beta) {
        private static final BinaryOperator<Candidate> PREFERRED = BinaryOperator.maxBy(
                Comparator.comparing((Candidate candidate) -> !candidate.beta()).thenComparingLong(Candidate::rank));
    }

    private final Map<String, Candidate> candidatesByRelease;

    private NeoForgeDistribution(Map<String, Candidate> candidatesByRelease) {
        this.candidatesByRelease = candidatesByRelease;
    }

    public static NeoForgeDistribution load(MetadataClient client) throws IOException, InterruptedException {
        return fromMetadata(client.fetch(VERSIONS), client.fetch(LEGACY_VERSIONS));
    }

    static NeoForgeDistribution fromMetadata(JsonNode versions, JsonNode legacyVersions)
            throws MetadataFormatException {
        Map<String, Candidate> candidates = new HashMap<>();
        for (JsonNode version : JsonFields.require(versions, "versions")) {
            Matcher matcher = VERSION.matcher(version.asText());
            if (matcher.matches()) {
                int minor = Integer.parseInt(matcher.group(2));
                String release = "1." + matcher.group(1) + (minor == 0 ? "" : "." + minor);
                Candidate candidate = new Candidate(
                        ARTIFACT_PREFIX + version.asText(), Long.parseLong(matcher.group(3)), matcher.group(4) != null);
                candidates.merge(release, candidate, Candidate.PREFERRED);
            }
        }
        for (JsonNode version : JsonFields.require(legacyVersions, "versions")) {
            Matcher matcher = LEGACY_VERSION.matcher(version.asText());
            if (matcher.matches()) {
                long rank = (Long.parseLong(matcher.group(2)) * RANK_RADIX + Long.parseLong(matcher.group(3)))
                                * RANK_RADIX
                        + Long.parseLong(matcher.group(4));
                Candidate candidate = new Candidate(LEGACY_ARTIFACT_PREFIX + version.asText(), rank, false);
                candidates.merge(matcher.group(1), candidate, Candidate.PREFERRED);
            }
        }
        return new NeoForgeDistribution(candidates);
    }

    @Override
    public Optional<LoaderTarget> targetFor(RequiredRelease release) {
        return Optional.ofNullable(candidatesByRelease.get(release.id()))
                .map(candidate -> new LoaderTarget(
                        LoaderTarget.Kind.NEOFORGE,
                        candidate.artifact(),
                        candidate.beta() ? LoaderTarget.Channel.BETA : LoaderTarget.Channel.STABLE,
                        LoaderTarget.Bootstrap.MODLAUNCHER,
                        null,
                        LoaderTarget.Toolchain.NEOGRADLE));
    }
}
