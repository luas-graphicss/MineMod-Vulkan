package io.github.luasgraphicss.minemod.compatibility.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.NativeBlock;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.OpenDescriptors;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class ContextsOnZinkTest {
    private static final int GL_NO_ERROR = 0;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int CHANNELS = 4;
    private static final int WARM_UP_CYCLES = 5;
    private static final int LIFECYCLE_CYCLES = 100;
    private static final long OTHER_THREAD_TIMEOUT_SECONDS = 30;

    @TempDir
    Path providerDirectory;

    private CompatibilitySession session;
    private SessionContext first;

    @BeforeEach
    void openOneContext() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        first = session.createContext(ProviderTestEnvironment.standalonePlan());
    }

    @AfterEach
    void closeTheSession() {
        first.close();
        session.close();
    }

    @Test
    void aTextureIsSharedWithASecondContextAndAFramebufferIsNot() {
        GlScene firstScene = entered(first);
        int texture = greenTexture(firstScene);
        int framebuffer = firstScene.generate("glGenFramebuffers");
        firstScene.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        firstScene.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);
        firstScene.call("glFinish");

        try (SessionContext second = session.createContextSharingWith(first, ProviderTestEnvironment.standalonePlan())) {
            GlScene secondScene = entered(second);

            assertEquals(1, secondScene.call("glIsTexture", texture));
            assertEquals(0, secondScene.call("glIsFramebuffer", framebuffer));
            assertArrayEquals(GlScene.GREEN, texelThroughAFramebuffer(secondScene, texture));
            assertEquals(GL_NO_ERROR, secondScene.call("glGetError"));
        }
    }

    @Test
    void aContextCurrentOnOneThreadIsRefusedOnAnother() {
        first.makeCurrent();

        IllegalStateException refused =
                assertThrows(IllegalStateException.class, () -> onAnotherThread(first::makeCurrent));

        assertTrue(refused.getMessage().contains("is current on thread"), refused.getMessage());
    }

    @Test
    void aReleasedContextDrawsOnTheThreadThatTakesIt() {
        first.release();

        onAnotherThread(() -> {
            GlScene scene = entered(first);
            scene.clear(GlScene.GREEN);
            assertArrayEquals(GlScene.GREEN, scene.pixelAt(0, 0));
            first.release();
        });

        assertFalse(session.contexts().current().isPresent());
    }

    @Test
    void closingTheFirstContextOfAShareGroupLeavesTheSecondUsable() {
        GlScene firstScene = entered(first);
        int texture = greenTexture(firstScene);
        firstScene.call("glFinish");
        SessionContext second = session.createContextSharingWith(first, ProviderTestEnvironment.standalonePlan());

        first.close();
        GlScene secondScene = entered(second);

        assertArrayEquals(GlScene.GREEN, texelThroughAFramebuffer(secondScene, texture));
        assertEquals(1, session.contexts().liveContextCount());
        second.close();
        assertEquals(0, session.contexts().liveContextCount());
        assertEquals(0, session.contexts().liveShareGroupCount());
    }

    @Test
    void contextsCreatedAndClosedInALoopLeaveNoContextObjectOrDescriptorBehind() throws IOException {
        drawInShortLivedContexts(WARM_UP_CYCLES);
        long descriptorsBefore = OpenDescriptors.count();

        drawInShortLivedContexts(LIFECYCLE_CYCLES);

        assertEquals(descriptorsBefore, OpenDescriptors.count());
        assertEquals(1, session.contexts().liveContextCount());
        assertEquals(1, session.contexts().liveShareGroupCount());
        assertEquals(0, session.contexts().liveObjectCount());
    }

    private void drawInShortLivedContexts(int cycles) {
        for (int cycle = 0; cycle < cycles; cycle++) {
            try (SessionContext shortLived = session.createContext(ProviderTestEnvironment.standalonePlan())) {
                GlScene scene = entered(shortLived);
                int texture = greenTexture(scene);
                assertArrayEquals(GlScene.GREEN, texelThroughAFramebuffer(scene, texture));
            }
        }
    }

    private static GlScene entered(SessionContext context) {
        context.makeCurrent();
        CompatibilityLayer layer = context.layer();
        layer.beginFrame();
        layer.enterCallback();
        return new GlScene(layer);
    }

    private static int greenTexture(GlScene scene) {
        int texture = scene.generate("glGenTextures");
        scene.call("glBindTexture", GL_TEXTURE_2D, texture);
        try (NativeBlock texel = new NativeBlock(CHANNELS)) {
            texel.setBytes(new byte[] {0, (byte) 0xFF, 0, (byte) 0xFF});
            scene.call("glTexImage2D", GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_BYTE, texel.address());
        }
        return texture;
    }

    private static int[] texelThroughAFramebuffer(GlScene scene, int texture) {
        int framebuffer = scene.generate("glGenFramebuffers");
        scene.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        scene.call("glFramebufferTexture2D", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        int[] texel = scene.pixelAt(0, 0);
        scene.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);
        return texel;
    }

    private static void onAnotherThread(Runnable work) {
        ExecutorService other = Executors.newSingleThreadExecutor();
        try {
            other.submit(work).get(OTHER_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException failure) {
            if (failure.getCause() instanceof RuntimeException) {
                throw (RuntimeException) failure.getCause();
            }
            if (failure.getCause() instanceof Error) {
                throw (Error) failure.getCause();
            }
            throw new IllegalStateException(failure.getCause());
        } catch (TimeoutException stuck) {
            throw new IllegalStateException("the other thread did not finish", stuck);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for the other thread", interrupted);
        } finally {
            other.shutdownNow();
        }
    }
}
