package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

class ClassNodeApplicationTest {
    private static final String HOOKS = "fixture/Hooks";

    private final ClassNodeApplication application =
            new ClassNodeApplication(FixtureGame.plan(FixtureGame.SWAP_BUFFERS), HOOKS);

    @Test
    void sendsThePinnedCallToTheHookOwner() {
        ClassNode window = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 1));

        application.rewrite(window);

        byte[] rewritten = classFileOf(window);
        assertEquals(1, FixtureGame.callsTo(rewritten, HOOKS));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.SWAP_BUFFERS.owner()));
    }

    @Test
    void namesTheClassesOfThePlan() {
        assertEquals(Collections.singleton(FixtureGame.WINDOW), application.rewrittenClasses());
    }

    @Test
    void refusesAClassThatMakesTheCallOnceMore() {
        ClassNode twoSwaps = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 2));

        PlanRefusedException refusal = assertThrows(PlanRefusedException.class, () -> application.rewrite(twoSwaps));

        assertTrue(refusal.getMessage().contains("matched 2 times"), refusal.getMessage());
    }

    @Test
    void refusesAClassWhoseCallAnotherTransformAlreadyTook() {
        ClassNode noSwap = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 0));

        PlanRefusedException refusal = assertThrows(PlanRefusedException.class, () -> application.rewrite(noSwap));

        assertTrue(refusal.getMessage().contains("matched nothing"), refusal.getMessage());
    }

    @Test
    void rejectsAClassOutsideThePlan() {
        ClassNode screen = new ClassNode();
        screen.name = "fixture/Screen";

        assertThrows(IllegalArgumentException.class, () -> application.rewrite(screen));
    }

    private static ClassNode nodeOf(byte[] classFile) {
        ClassNode node = new ClassNode();
        new ClassReader(classFile).accept(node, 0);
        return node;
    }

    private static byte[] classFileOf(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
