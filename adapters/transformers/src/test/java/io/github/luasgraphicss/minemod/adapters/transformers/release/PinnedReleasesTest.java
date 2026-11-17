package io.github.luasgraphicss.minemod.adapters.transformers.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.FixtureClasses;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PinnedReleasesTest {
    private static final String CLIENT_OF_1_13 = "c0b970952cdd279912da384cdbfc0c26e6c6090b";
    private static final String CLIENT_OF_1_13_1 = "8de235e5ec3a7fce168056ea395d21cbdec18d7c";
    private static final String UNPINNED_CLIENT = "0000000000000000000000000000000000000000";

    @Test
    void findsThePlanOfAPinnedClient() {
        PinnedReleases releases =
                new PinnedReleases(Arrays.asList(planOf("1.13", CLIENT_OF_1_13), planOf("1.13.1", CLIENT_OF_1_13_1)));

        assertEquals("1.13.1", releases.requirePlanFor(CLIENT_OF_1_13_1).release());
        assertEquals(Arrays.asList("1.13", "1.13.1"), releases.releases());
    }

    @Test
    void refusesAClientThatIsNotPinnedAndNamesTheSupportedReleases() {
        PinnedReleases releases = new PinnedReleases(Collections.singletonList(planOf("1.13", CLIENT_OF_1_13)));

        assertFalse(releases.planFor(UNPINNED_CLIENT).isPresent());
        UnsupportedClientException refusal =
                assertThrows(UnsupportedClientException.class, () -> releases.requirePlanFor(UNPINNED_CLIENT));
        assertEquals(UNPINNED_CLIENT, refusal.clientSha1());
        assertTrue(refusal.getMessage().contains("[1.13]"), refusal.getMessage());
    }

    @Test
    void refusesTwoReleasesPinnedToTheSameClient() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PinnedReleases(
                        Arrays.asList(planOf("1.13", CLIENT_OF_1_13), planOf("1.13.1", CLIENT_OF_1_13))));
    }

    private static ReleasePlan planOf(String release, String clientSha1) {
        return new ReleasePlan(
                release,
                clientSha1,
                Collections.singletonList(new CallSiteRedirect(
                        TransformPoint.hook("window-create", FixtureClasses.OPEN, 1), FixtureClasses.CREATE_WINDOW)));
    }
}
