package io.github.luasgraphicss.minemod.compatibility.resources.framebuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FramebufferBindingsTest {
    private static final int SCENE_BACKING = 900;

    private final NameTable framebuffers = new NameTable(ObjectKind.FRAMEBUFFER, 8);
    private final FramebufferBindings bindings = new FramebufferBindings(framebuffers, SCENE_BACKING);

    @Test
    void startsOnTheDefaultFramebuffer() {
        assertTrue(bindings.drawsToDefault());
        assertTrue(bindings.readsFromDefault());
        assertEquals(SCENE_BACKING, bindings.backingOf(FramebufferBindings.DEFAULT_FRAMEBUFFER));
    }

    @Test
    void bindingTheDefaultFramebufferSelectsTheSceneFramebuffer() {
        int offscreen = framebuffers.allocate(301);
        bindings.bind(FramebufferTarget.FRAMEBUFFER, offscreen);

        int backing = bindings.bind(FramebufferTarget.FRAMEBUFFER, FramebufferBindings.DEFAULT_FRAMEBUFFER);

        assertEquals(SCENE_BACKING, backing);
        assertEquals(FramebufferBindings.DEFAULT_FRAMEBUFFER, bindings.drawBinding());
        assertEquals(FramebufferBindings.DEFAULT_FRAMEBUFFER, bindings.readBinding());
    }

    @Test
    void theCombinedTargetMovesBothBindings() {
        int offscreen = framebuffers.allocate(301);

        assertEquals(301, bindings.bind(FramebufferTarget.FRAMEBUFFER, offscreen));
        assertEquals(offscreen, bindings.drawBinding());
        assertEquals(offscreen, bindings.readBinding());
    }

    @Test
    void theReadAndDrawTargetsMoveIndependently() {
        int source = framebuffers.allocate(301);
        int destination = framebuffers.allocate(302);

        bindings.bind(FramebufferTarget.READ_FRAMEBUFFER, source);
        assertTrue(bindings.drawsToDefault());
        bindings.bind(FramebufferTarget.DRAW_FRAMEBUFFER, destination);

        assertEquals(source, bindings.readBinding());
        assertEquals(destination, bindings.drawBinding());
        assertFalse(bindings.readsFromDefault());
    }

    @Test
    void anUnknownNameLeavesTheBindingsUntouched() {
        int offscreen = framebuffers.allocate(301);
        bindings.bind(FramebufferTarget.FRAMEBUFFER, offscreen);

        assertThrows(IllegalArgumentException.class, () -> bindings.bind(FramebufferTarget.FRAMEBUFFER, 7));
        assertEquals(offscreen, bindings.drawBinding());
        assertEquals(offscreen, bindings.readBinding());
    }

    @Test
    void deletingABoundFramebufferRevertsItsTargetsToTheDefault() {
        int offscreen = framebuffers.allocate(301);
        int other = framebuffers.allocate(302);
        bindings.bind(FramebufferTarget.DRAW_FRAMEBUFFER, offscreen);
        bindings.bind(FramebufferTarget.READ_FRAMEBUFFER, other);

        assertEquals(EnumSet.of(FramebufferTarget.DRAW_FRAMEBUFFER), bindings.revertBindingsOf(offscreen));
        assertTrue(bindings.drawsToDefault());
        assertEquals(other, bindings.readBinding());
    }

    @Test
    void deletingAnUnboundOrDefaultFramebufferRevertsNothing() {
        int offscreen = framebuffers.allocate(301);

        assertEquals(Collections.emptySet(), bindings.revertBindingsOf(offscreen));
        assertEquals(Collections.emptySet(), bindings.revertBindingsOf(FramebufferBindings.DEFAULT_FRAMEBUFFER));
    }

    @Test
    void recognizesOnlyTheThreeFramebufferTargets() {
        assertEquals(Optional.of(FramebufferTarget.FRAMEBUFFER), FramebufferTarget.fromCode(0x8D40));
        assertEquals(Optional.of(FramebufferTarget.READ_FRAMEBUFFER), FramebufferTarget.fromCode(0x8CA8));
        assertEquals(Optional.of(FramebufferTarget.DRAW_FRAMEBUFFER), FramebufferTarget.fromCode(0x8CA9));
        assertEquals(Optional.empty(), FramebufferTarget.fromCode(0x8D41));
    }

    @Test
    void rejectsATableOfAnotherKind() {
        NameTable textures = new NameTable(ObjectKind.TEXTURE, 8);

        assertThrows(IllegalArgumentException.class, () -> new FramebufferBindings(textures, SCENE_BACKING));
    }
}
