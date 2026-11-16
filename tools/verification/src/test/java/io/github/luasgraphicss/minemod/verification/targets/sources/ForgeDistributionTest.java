package io.github.luasgraphicss.minemod.verification.targets.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ForgeDistributionTest {
    private static final String VERSIONS = """
            {
              "1.12.2": ["1.12.2-14.23.5.2859", "1.12.2-14.23.5.2864"],
              "1.13.2": ["1.13.2-25.0.219", "1.13.2-25.0.223"],
              "1.14.4": ["1.14.4-28.2.26"]
            }
            """;
    private static final String PROMOTIONS = """
            {
              "promos": {
                "1.12.2-recommended": "14.23.5.2859",
                "1.12.2-latest": "14.23.5.2864",
                "1.13.2-latest": "25.0.223"
              }
            }
            """;

    @Test
    void pinsTheRecommendedBuildWhenForgePromotesOne() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.12.2", "1.12", 8)).orElseThrow();

        assertEquals("net.minecraftforge:forge:1.12.2-14.23.5.2859", target.artifact());
        assertEquals(LoaderTarget.Channel.RECOMMENDED, target.channel());
        assertEquals(LoaderTarget.Bootstrap.LAUNCHWRAPPER, target.bootstrap());
    }

    @Test
    void pinsTheLatestBuildWhenNoneIsRecommended() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.13.2", "1.13", 8)).orElseThrow();

        assertEquals("net.minecraftforge:forge:1.13.2-25.0.223", target.artifact());
        assertEquals(LoaderTarget.Channel.LATEST, target.channel());
        assertEquals(LoaderTarget.Bootstrap.MODLAUNCHER, target.bootstrap());
    }

    @Test
    void pinsTheNewestPublishedBuildWhenNothingIsPromoted() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.14.4", "1.14", 8)).orElseThrow();

        assertEquals("net.minecraftforge:forge:1.14.4-28.2.26", target.artifact());
        assertEquals(LoaderTarget.Channel.LATEST, target.channel());
    }

    @Test
    void offersNothingForAReleaseForgeNeverPublished() throws IOException {
        assertEquals(Optional.empty(), distribution().targetFor(new RequiredRelease("1.13.1", "1.13", 8)));
    }

    private static ForgeDistribution distribution() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return ForgeDistribution.fromMetadata(mapper.readTree(VERSIONS), mapper.readTree(PROMOTIONS));
    }
}
