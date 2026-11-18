package io.github.luasgraphicss.minemod.adapters.loaders.modlauncher.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cpw.mods.modlauncher.api.ITransformer;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassNodeApplications;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.InstalledOwners;
import io.github.luasgraphicss.minemod.adapters.presentation.glfw.GlfwPresentationHooks;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.PresentationOwners;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

class PinnedFrameServiceTest {
    private final PinnedFrameService service = new PinnedFrameService(applications());

    @AfterEach
    void releaseTheOwner() {
        InstalledOwners.release();
    }

    @Test
    void sendsThePinnedWindowCallToTheHooksAndThePinnedOpenGlCallToItsStandIn() {
        ClassNode window = nodeOf(FixtureGame.windowCallingEach(FixtureGame.SWAP_BUFFERS, FixtureGame.MATRIX_MODE));

        ClassNode transformed = onlyTransformer().transform(window, null);

        assertSame(window, transformed);
        ClassWriter writer = new ClassWriter(0);
        transformed.accept(writer);
        byte[] rewritten = writer.toByteArray();
        assertEquals(1, FixtureGame.callsTo(rewritten, GlfwPresentationHooks.INTERNAL_NAME));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.SWAP_BUFFERS.owner()));
        assertEquals(1, FixtureGame.callsTo(rewritten, FixtureGame.STAND_IN));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.MATRIX_MODE.owner()));
    }

    @Test
    void refusesAClassThatLostAPinnedOpenGlCall() {
        ClassNode withoutTheMatrixCall = nodeOf(FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 1));

        assertThrows(PlanRefusedException.class, () -> onlyTransformer().transform(withoutTheMatrixCall, null));
    }

    @Test
    void installsTheOwnerOfTheRecordedFrameWhenModLauncherInitializesTheService() {
        assertFalse(PresentationOwners.isInstalled());

        service.initialize(null);

        assertTrue(PresentationOwners.isInstalled());
        assertFalse(PresentationOwners.current().keepsOpenGlContext());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ITransformer<ClassNode> onlyTransformer() {
        List<ITransformer> transformers = service.transformers();
        assertEquals(1, transformers.size());
        return transformers.get(0);
    }

    private static ClassNodeApplications applications() {
        String text =
                FixtureGame.framePlanText(FixtureGame.MATRIX_MODE, FixtureGame.UNPINNED_CLIENT, FixtureGame.STAND_IN);
        try {
            return FramePlan.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))
                    .classNodesAfter(FixtureGame.plan(FixtureGame.SWAP_BUFFERS), GlfwPresentationHooks.INTERNAL_NAME);
        } catch (IOException unreadable) {
            throw new IllegalStateException(unreadable);
        }
    }

    private static ClassNode nodeOf(byte[] classFile) {
        ClassNode node = new ClassNode();
        new ClassReader(classFile).accept(node, 0);
        return node;
    }
}
