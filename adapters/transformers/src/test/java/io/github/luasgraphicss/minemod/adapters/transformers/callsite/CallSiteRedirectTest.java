package io.github.luasgraphicss.minemod.adapters.transformers.callsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.FixtureClasses;
import io.github.luasgraphicss.minemod.adapters.transformers.fixtures.WindowHooks;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class CallSiteRedirectTest {
    @Test
    void refusesAPointThatReplacesItsMethod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CallSiteRedirect(
                        TransformPoint.replacement("window-create", FixtureClasses.OPEN),
                        FixtureClasses.CREATE_WINDOW));
    }

    @Test
    void findsNoMissingHookWhenTheOwnerDeclaresEveryCallee() {
        assertEquals(
                Collections.emptySet(),
                HookOwnerContract.findMissingHooks(
                        WindowHooks.class,
                        Arrays.asList(
                                redirect("window-create", FixtureClasses.CREATE_WINDOW),
                                redirect("swap-buffers", FixtureClasses.SWAP_BUFFERS))));
    }

    @Test
    void namesTheHooksTheOwnerDoesNotDeclare() {
        assertEquals(
                new TreeSet<>(Collections.singletonList("pollEvents()V")),
                HookOwnerContract.findMissingHooks(
                        WindowHooks.class,
                        Arrays.asList(
                                redirect("window-create", FixtureClasses.CREATE_WINDOW),
                                redirect("poll-events", FixtureClasses.POLL_EVENTS))));
    }

    private static CallSiteRedirect redirect(
            String id, io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget callee) {
        return new CallSiteRedirect(TransformPoint.hook(id, FixtureClasses.FLIP_TWICE, 1), callee);
    }
}
