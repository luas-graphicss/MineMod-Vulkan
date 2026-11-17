package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.TargetType;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

class PinnedCallsServiceTest {
    private final PinnedCallsService service = new PinnedCallsService(
            new ClassNodeApplication(FixtureGame.plan(FixtureGame.SWAP_BUFFERS), GlfwPresentationHooks.INTERNAL_NAME));

    @Test
    void targetsExactlyTheClassesOfThePlan() {
        PinnedClassTransformer transformer = onlyTransformer();

        assertEquals(Set.of(ITransformer.Target.targetClass(FixtureGame.WINDOW)), transformer.targets());
        assertSame(TargetType.CLASS, transformer.getTargetType());
        assertEquals(TransformerVoteResult.YES, transformer.castVote(null));
    }

    @Test
    void sendsThePinnedCallToTheHooks() {
        ClassNode window = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 1));

        ClassNode transformed = onlyTransformer().transform(window, null);

        assertSame(window, transformed);
        ClassWriter writer = new ClassWriter(0);
        transformed.accept(writer);
        assertEquals(1, FixtureGame.callsTo(writer.toByteArray(), GlfwPresentationHooks.INTERNAL_NAME));
        assertEquals(0, FixtureGame.callsTo(writer.toByteArray(), FixtureGame.SWAP_BUFFERS.owner()));
    }

    @Test
    void refusesAClassThatMakesTheCallOnceMore() {
        ClassNode twoSwaps = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 2));

        assertThrows(PlanRefusedException.class, () -> onlyTransformer().transform(twoSwaps, null));
    }

    private PinnedClassTransformer onlyTransformer() {
        List<? extends ITransformer<?>> transformers = service.transformers();
        assertEquals(1, transformers.size());
        return (PinnedClassTransformer) transformers.get(0);
    }

    private static ClassNode nodeOf(byte[] classFile) {
        ClassNode node = new ClassNode();
        new ClassReader(classFile).accept(node, 0);
        return node;
    }
}
