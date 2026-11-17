package io.github.luasgraphicss.minemod.adapters.loaders.launch.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PinnedPlanTest {
    @TempDir
    Path adapterRoot;

    @Test
    void readsThePlanTheAdapterCarries() throws IOException {
        FixtureGame.writePlan(adapterRoot, FixtureGame.SWAP_BUFFERS, FixtureGame.UNPINNED_CLIENT);

        try (URLClassLoader adapter = adapterLoader()) {
            ReleasePlan plan = PinnedPlan.of(adapter);

            assertEquals(FixtureGame.RELEASE, plan.release());
            assertEquals(1, plan.redirects().size());
        }
    }

    @Test
    void refusesAnAdapterWithoutAPlan() throws IOException {
        try (URLClassLoader adapter = adapterLoader()) {
            IllegalStateException refusal = assertThrows(IllegalStateException.class, () -> PinnedPlan.of(adapter));

            assertTrue(refusal.getMessage().contains(PinnedPlan.RESOURCE), refusal.getMessage());
        }
    }

    private URLClassLoader adapterLoader() throws IOException {
        return new URLClassLoader(new URL[] {adapterRoot.toUri().toURL()}, null);
    }
}
