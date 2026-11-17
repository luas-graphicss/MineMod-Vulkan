package io.github.luasgraphicss.minemod.verification.hooks.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.transformers.release.PinnedReleases;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.release.UnsupportedClientException;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.CommittedManifests;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ReleasePlansTest {
    @Test
    void everyPinnedClientSelectsThePlanOfItsRelease() throws IOException {
        PinnedReleases pinned = ReleasePlans.pinnedBy(CommittedManifests.hooks());

        for (ReleaseTarget release : CommittedManifests.targets().releases()) {
            ReleasePlan plan = pinned.requirePlanFor(release.client().sha1());
            assertEquals(release.id(), plan.release());
            assertEquals(plan.redirects().size(), plan.transformPlan().points().size());
        }
        assertEquals(37, pinned.releases().size());
    }

    @Test
    void refusesAClientThatIsNotPinned() throws IOException {
        PinnedReleases pinned = ReleasePlans.pinnedBy(CommittedManifests.hooks());

        assertThrows(
                UnsupportedClientException.class,
                () -> pinned.requirePlanFor("0123456789abcdef0123456789abcdef01234567"));
    }
}
