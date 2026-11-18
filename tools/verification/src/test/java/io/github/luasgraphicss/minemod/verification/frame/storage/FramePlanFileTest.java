package io.github.luasgraphicss.minemod.verification.frame.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.plan.RuntimePlans;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.fixtures.FrameFixtures;
import io.github.luasgraphicss.minemod.verification.frame.plan.FramePoints;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile.Committed;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FramePlanFileTest {
    @TempDir
    Path directory;

    private StandIns standIns;
    private String rendered;

    @BeforeEach
    void renderThePlanOfAClient() throws IOException {
        standIns = StandIns.ofLwjglGeneration(FrameFixtures.LWJGL_GENERATION);
        try (ClientArchive client = FrameFixtures.client(directory.resolve("client.jar"))) {
            AdapterPlan official = AdapterPlan.written(
                    RuntimePlans.written(
                            FramePoints.pinnedOn(FrameFixtures.release(), client, standIns), RuntimeNamespace.OFFICIAL),
                    RuntimeNamespace.OFFICIAL);
            rendered = FramePlanFile.render(official, standIns);
        }
    }

    @Test
    void writesWhatTheLoaderReadsAtLaunch() throws IOException {
        Committed committed = FramePlanFile.read(fileHolding(rendered));

        assertEquals(Set.of(FrameFixtures.MATRIX_STAND_IN), committed.atLaunch().plansByStandIn().keySet());
        assertEquals(3, committed.names().plan().redirects().size());
        assertEquals(RuntimeNamespace.OFFICIAL, committed.names().namespace());
        assertEquals(rendered, FramePlanFile.render(committed.names(), standIns));
    }

    @Test
    void refusesAPlanWhosePointsNameNoStandIn() throws IOException {
        String withoutStandIns = rendered.lines()
                .filter(line -> !line.contains(".hook="))
                .collect(Collectors.joining("\n", "", "\n"));
        Path file = fileHolding(withoutStandIns);

        IOException refusal = assertThrows(IOException.class, () -> FramePlanFile.read(file));

        assertTrue(refusal.getMessage().contains(".hook names no class that answers the call"), refusal.getMessage());
    }

    private Path fileHolding(String text) throws IOException {
        return Files.writeString(directory.resolve("frame-plan.properties"), text);
    }
}
