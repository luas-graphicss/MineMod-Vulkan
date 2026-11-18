package io.github.luasgraphicss.minemod.adapters.loaders.knot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NativeOwnerOnLoadTest {
    private static final String MIXIN_PACKAGE = "fixture.targets";

    private final PinnedCallsPlugin plugin = new PinnedCallsPlugin(
            new ClassNodeApplication(FixtureGame.plan(FixtureGame.SWAP_BUFFERS), GlfwPresentationHooks.INTERNAL_NAME));

    @AfterEach
    void releaseTheOwner() {
        InstalledOwners.release();
    }

    @Test
    void installsTheNativeOwnerWhenMixinLoadsThePlugin() {
        assertFalse(PresentationOwners.isInstalled());

        plugin.onLoad(MIXIN_PACKAGE);

        assertTrue(PresentationOwners.isInstalled());
        assertFalse(PresentationOwners.current().keepsOpenGlContext());
    }
}
