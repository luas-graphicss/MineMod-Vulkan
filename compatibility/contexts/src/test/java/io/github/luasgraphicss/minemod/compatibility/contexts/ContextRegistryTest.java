package io.github.luasgraphicss.minemod.compatibility.contexts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ContextRegistryTest {
    private final RecordingBackend backend = new RecordingBackend();
    private final ContextRegistry registry = new ContextRegistry(backend, 16);
    private final ExecutorService otherThread = Executors.newSingleThreadExecutor();

    @AfterEach
    void stopTheOtherThread() {
        otherThread.shutdownNow();
    }

    @Test
    void aNewContextStartsItsOwnShareGroup() {
        CompatibilityContext first = registry.create();
        CompatibilityContext second = registry.create();

        assertNotSame(first.shareGroup(), second.shareGroup());
        assertEquals(2, registry.liveContextCount());
        assertEquals(2, registry.liveShareGroupCount());
    }

    @Test
    void aSharingContextSeesTheObjectsOfItsGroup() {
        CompatibilityContext first = registry.create();
        CompatibilityContext second = registry.createSharingWith(first);

        int texture = first.names().table(ObjectKind.TEXTURE).allocate(101);

        assertSame(first.shareGroup(), second.shareGroup());
        assertEquals(101, second.names().table(ObjectKind.TEXTURE).backingOf(texture));
        assertEquals(first.handle(), backend.shareSourceOf(second.handle()));
        assertEquals(1, registry.liveShareGroupCount());
    }

    @Test
    void aSharingContextKeepsItsOwnFramebuffers() {
        CompatibilityContext first = registry.create();
        CompatibilityContext second = registry.createSharingWith(first);

        int framebuffer = first.names().table(ObjectKind.FRAMEBUFFER).allocate(201);

        assertFalse(second.names().table(ObjectKind.FRAMEBUFFER).contains(framebuffer));
    }

    @Test
    void makesAContextCurrentOnTheCallingThread() {
        CompatibilityContext context = registry.create();

        registry.makeCurrent(context);

        assertSame(context, registry.requireCurrent());
        assertEquals(Optional.of(context), registry.current());
        assertEquals(OptionalLong.of(context.handle()), backend.currentOnCallingThread());
    }

    @Test
    void refusesToMakeAContextCurrentOnASecondThread() throws Exception {
        CompatibilityContext context = registry.create();
        onTheOtherThread(() -> registry.makeCurrent(context));

        assertThrows(IllegalStateException.class, () -> registry.makeCurrent(context));

        onTheOtherThread(registry::releaseCurrent);
        registry.makeCurrent(context);
        assertSame(context, registry.requireCurrent());
    }

    @Test
    void aThreadThatMadeNothingCurrentHasNoContext() throws Exception {
        CompatibilityContext context = registry.create();
        onTheOtherThread(() -> registry.makeCurrent(context));

        assertEquals(Optional.empty(), registry.current());
        assertThrows(IllegalStateException.class, registry::requireCurrent);
    }

    @Test
    void makingAnotherContextCurrentReleasesThePreviousOne() throws Exception {
        CompatibilityContext first = registry.create();
        CompatibilityContext second = registry.create();
        registry.makeCurrent(first);

        registry.makeCurrent(second);

        assertSame(second, registry.requireCurrent());
        onTheOtherThread(() -> registry.makeCurrent(first));
    }

    @Test
    void sharedObjectsLiveUntilTheLastContextOfTheGroupIsDestroyed() {
        CompatibilityContext first = registry.create();
        CompatibilityContext second = registry.createSharingWith(first);
        first.names().table(ObjectKind.TEXTURE).allocate(101);

        registry.destroy(first);
        assertEquals(1, registry.liveObjectCount());

        registry.destroy(second);
        assertEquals(0, second.shareGroup().sharedNames().liveCount());
        assertEquals(0, registry.liveShareGroupCount());
        assertEquals(0, registry.liveContextCount());
        assertEquals(0, backend.liveCount());
    }

    @Test
    void destroyingTheCurrentContextReleasesItFirst() {
        CompatibilityContext context = registry.create();
        registry.makeCurrent(context);
        context.names().table(ObjectKind.FRAMEBUFFER).allocate(201);

        registry.destroy(context);

        assertFalse(registry.isLive(context));
        assertEquals(Optional.empty(), registry.current());
        assertEquals(OptionalLong.empty(), backend.currentOnCallingThread());
        assertEquals(0, backend.liveCount());
    }

    @Test
    void aContextCurrentOnAnotherThreadIsDestroyedWhenThatThreadReleasesIt() throws Exception {
        CompatibilityContext context = registry.create();
        onTheOtherThread(() -> registry.makeCurrent(context));

        registry.destroy(context);
        assertTrue(registry.isLive(context));
        assertEquals(1, backend.liveCount());

        onTheOtherThread(registry::releaseCurrent);
        assertFalse(registry.isLive(context));
        assertEquals(0, backend.liveCount());
        assertEquals(0, registry.liveContextCount());
    }

    @Test
    void aDestroyedContextCannotBeUsedAgain() {
        CompatibilityContext context = registry.create();
        registry.destroy(context);

        assertThrows(IllegalStateException.class, () -> registry.makeCurrent(context));
        assertThrows(IllegalStateException.class, () -> registry.destroy(context));
        assertThrows(IllegalStateException.class, () -> registry.createSharingWith(context));
    }

    @Test
    void aRefusedCreationLeavesNothingBehind() {
        backend.refuseCreation();

        assertThrows(IllegalStateException.class, registry::create);
        assertEquals(0, registry.liveContextCount());
        assertEquals(0, registry.liveShareGroupCount());
    }

    @Test
    void shutdownRefusesWhileAContextIsCurrentOnAnotherThread() throws Exception {
        CompatibilityContext held = registry.create();
        registry.create();
        onTheOtherThread(() -> registry.makeCurrent(held));

        assertThrows(IllegalStateException.class, registry::shutdown);
        assertEquals(2, registry.liveContextCount());

        onTheOtherThread(registry::releaseCurrent);
        registry.shutdown();
        assertEquals(0, registry.liveContextCount());
        assertEquals(0, backend.liveCount());
    }

    @Test
    void shutdownDestroysIdleAndCurrentContexts() {
        CompatibilityContext current = registry.create();
        registry.createSharingWith(current);
        registry.makeCurrent(current);

        registry.shutdown();

        assertEquals(0, registry.liveContextCount());
        assertEquals(0, registry.liveShareGroupCount());
        assertEquals(0, backend.liveCount());
        assertEquals(Optional.empty(), registry.current());
    }

    private void onTheOtherThread(Runnable action) throws Exception {
        otherThread.submit(action).get();
    }
}
