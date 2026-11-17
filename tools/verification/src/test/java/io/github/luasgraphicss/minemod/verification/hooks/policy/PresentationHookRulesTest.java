package io.github.luasgraphicss.minemod.verification.hooks.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.hooks.fixtures.CommittedManifests;
import io.github.luasgraphicss.minemod.verification.hooks.model.HookPoint;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class PresentationHookRulesTest {
    private static final String OTHER_CLIENT = "0123456789abcdef0123456789abcdef01234567";

    @Test
    void committedHooksSatisfyTheRules() throws IOException {
        assertEquals(List.of(), violationsOf(CommittedManifests.hooks()));
    }

    @Test
    void committedHooksPinEveryRelease() throws IOException {
        PresentationHooks hooks = CommittedManifests.hooks();

        assertEquals(37, hooks.releases().size());
        assertTrue(hooks.releases().stream().allMatch(release -> release.callSites() >= release.points().size()));
    }

    @Test
    void rejectsHooksPinnedOnAnotherClient() throws IOException {
        PresentationHooks hooks = changeRelease(
                "1.13", release -> new ReleaseHooks(release.release(), OTHER_CLIENT, release.windowing(), release.points()));

        List<String> violations = violationsOf(hooks);

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).startsWith("release 1.13: hooks were pinned on client " + OTHER_CLIENT));
    }

    @Test
    void rejectsAReleaseThatLeavesACallUnpinned() throws IOException {
        PresentationHooks hooks = changeRelease("1.21.1", release -> withoutHook(release, "swap-buffers"));

        assertEquals(
                List.of("release 1.21.1: no call of org/lwjgl/glfw/GLFW.glfwSwapBuffers(J)V is pinned for swap-buffers"),
                violationsOf(hooks));
    }

    @Test
    void rejectsAPointThatRedirectsAnotherCall() throws IOException {
        PresentationHooks hooks = changeRelease("1.16.5", release -> {
            List<HookPoint> points = new ArrayList<>(release.points());
            HookPoint first = points.get(0);
            points.set(0, new HookPoint(first.id(), first.hook(), "java/lang/System.exit(I)V", first.within(), null, 1));
            return new ReleaseHooks(release.release(), release.clientSha1(), release.windowing(), points);
        });

        List<String> violations = violationsOf(hooks);

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).contains("redirects java/lang/System.exit(I)V"), violations.toString());
    }

    @Test
    void rejectsHooksThatSkipARelease() throws IOException {
        PresentationHooks committed = CommittedManifests.hooks();
        List<ReleaseHooks> releases = new ArrayList<>(committed.releases());
        releases.removeIf(release -> release.release().equals("1.12.2"));

        List<String> violations = violationsOf(new PresentationHooks(committed.schemaVersion(), releases));

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.get(0).startsWith("hooks are pinned for [1.13, "), violations.toString());
    }

    private static List<String> violationsOf(PresentationHooks hooks) throws IOException {
        return PresentationHookRules.findViolations(hooks, CommittedManifests.targets());
    }

    private static PresentationHooks changeRelease(String id, UnaryOperator<ReleaseHooks> change) throws IOException {
        PresentationHooks committed = CommittedManifests.hooks();
        List<ReleaseHooks> releases = committed.releases().stream()
                .map(release -> release.release().equals(id) ? change.apply(release) : release)
                .toList();
        return new PresentationHooks(committed.schemaVersion(), releases);
    }

    private static ReleaseHooks withoutHook(ReleaseHooks release, String hook) {
        List<HookPoint> points = release.points().stream().filter(point -> !point.hook().equals(hook)).toList();
        return new ReleaseHooks(release.release(), release.clientSha1(), release.windowing(), points);
    }
}
