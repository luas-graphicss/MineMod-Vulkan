package io.github.luasgraphicss.minemod.verification.targets.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NeoForgeDistributionTest {
    private static final String VERSIONS = """
            {
              "isSnapshot": false,
              "versions": [
                "0.25w14craftmine.3-beta",
                "20.5.20-beta", "20.5.21-beta",
                "21.0.167",
                "21.1.9", "21.1.265", "21.1.266", "21.1.300-beta",
                "26.3.0.101"
              ]
            }
            """;
    private static final String LEGACY_VERSIONS = """
            {"isSnapshot": false, "versions": ["1.20.1-47.1.5", "1.20.1-47.1.106", "1.20.1-47.1.99"]}
            """;

    @Test
    void pinsTheNewestStableBuildOfARelease() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.21.1", "1.21", 21)).orElseThrow();

        assertEquals("net.neoforged:neoforge:21.1.266", target.artifact());
        assertEquals(LoaderTarget.Channel.STABLE, target.channel());
    }

    @Test
    void mapsAZeroMinorToTheFirstReleaseOfTheFamily() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.21", "1.21", 21)).orElseThrow();

        assertEquals("net.neoforged:neoforge:21.0.167", target.artifact());
    }

    @Test
    void keepsTheBetaChannelWhenAReleaseOnlyHasBetas() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.20.5", "1.20", 21)).orElseThrow();

        assertEquals("net.neoforged:neoforge:20.5.21-beta", target.artifact());
        assertEquals(LoaderTarget.Channel.BETA, target.channel());
    }

    @Test
    void pinsTheNewestLegacyCoordinateForTheFirstSupportedRelease() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.20.1", "1.20", 17)).orElseThrow();

        assertEquals("net.neoforged:forge:1.20.1-47.1.106", target.artifact());
    }

    @Test
    void offersNothingForReleasesOlderThanTheProject() throws IOException {
        assertEquals(Optional.empty(), distribution().targetFor(new RequiredRelease("1.19.4", "1.19", 17)));
    }

    private static NeoForgeDistribution distribution() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return NeoForgeDistribution.fromMetadata(mapper.readTree(VERSIONS), mapper.readTree(LEGACY_VERSIONS));
    }
}
