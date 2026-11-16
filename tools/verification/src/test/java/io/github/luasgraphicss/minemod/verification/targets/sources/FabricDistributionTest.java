package io.github.luasgraphicss.minemod.verification.targets.sources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.policy.ReleaseCatalog.RequiredRelease;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FabricDistributionTest {
    private static final String GAMES = """
            [{"version": "1.21.1", "stable": true}, {"version": "1.14", "stable": true}]
            """;
    private static final String LOADERS = """
            [
              {"maven": "net.fabricmc:fabric-loader:0.20.2", "version": "0.20.2", "stable": false},
              {"maven": "net.fabricmc:fabric-loader:0.20.1", "version": "0.20.1", "stable": true},
              {"maven": "net.fabricmc:fabric-loader:0.19.0", "version": "0.19.0", "stable": true}
            ]
            """;
    private static final String INTERMEDIARIES = """
            [
              {"maven": "net.fabricmc:intermediary:1.21.1", "version": "1.21.1", "stable": true},
              {"maven": "net.fabricmc:intermediary:1.14", "version": "1.14", "stable": true}
            ]
            """;

    @Test
    void pinsTheNewestStableLoaderWithTheIntermediaryOfTheRelease() throws IOException {
        LoaderTarget target = distribution().targetFor(new RequiredRelease("1.21.1", "1.21", 21)).orElseThrow();

        assertEquals("net.fabricmc:fabric-loader:0.20.1", target.artifact());
        assertEquals("net.fabricmc:intermediary:1.21.1", target.mappings());
        assertEquals(LoaderTarget.Bootstrap.FABRIC_KNOT, target.bootstrap());
    }

    @Test
    void offersNothingForAReleaseFabricDoesNotList() throws IOException {
        assertEquals(Optional.empty(), distribution().targetFor(new RequiredRelease("1.13.2", "1.13", 8)));
    }

    private static FabricDistribution distribution() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return FabricDistribution.fromMetadata(
                mapper.readTree(GAMES), mapper.readTree(LOADERS), mapper.readTree(INTERMEDIARIES));
    }
}
