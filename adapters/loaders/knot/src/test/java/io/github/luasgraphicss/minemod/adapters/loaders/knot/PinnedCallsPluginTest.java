package io.github.luasgraphicss.minemod.adapters.loaders.knot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

class PinnedCallsPluginTest {
    private static final String WINDOW_NAME = "fixture.Window";
    private static final String TARGETS_MIXIN = "fixture.targets.PinnedCallSites";

    private final PinnedCallsPlugin plugin = new PinnedCallsPlugin(
            new ClassNodeApplication(FixtureGame.plan(FixtureGame.SWAP_BUFFERS), GlfwPresentationHooks.INTERNAL_NAME));

    @Test
    void sendsThePinnedCallToTheHooksOnceTheMixinsOfTheClassWereApplied() {
        ClassNode window = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 1));

        plugin.postApply(WINDOW_NAME, window, TARGETS_MIXIN, null);

        ClassWriter writer = new ClassWriter(0);
        window.accept(writer);
        assertEquals(1, FixtureGame.callsTo(writer.toByteArray(), GlfwPresentationHooks.INTERNAL_NAME));
        assertEquals(0, FixtureGame.callsTo(writer.toByteArray(), FixtureGame.SWAP_BUFFERS.owner()));
    }

    @Test
    void refusesAClassWhoseCallAnotherMixinRedirected() {
        ClassNode redirectedByAnotherMod = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 0));

        assertThrows(
                PlanRefusedException.class,
                () -> plugin.postApply(WINDOW_NAME, redirectedByAnotherMod, TARGETS_MIXIN, null));
    }

    private static ClassNode nodeOf(byte[] classFile) {
        ClassNode node = new ClassNode();
        new ClassReader(classFile).accept(node, 0);
        return node;
    }
}
