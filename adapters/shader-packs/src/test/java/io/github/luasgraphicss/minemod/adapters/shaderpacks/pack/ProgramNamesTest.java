package io.github.luasgraphicss.minemod.adapters.shaderpacks.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class ProgramNamesTest {
    @Test
    void geometryFallsBackAlongTheChainOfItsProgram() {
        assertEquals(
                Optional.of("gbuffers_textured"),
                GeometryProgram.TERRAIN.resolveAmong(
                        new HashSet<>(Arrays.asList("gbuffers_textured", "gbuffers_basic"))));
        assertEquals(
                Optional.of("gbuffers_terrain"),
                GeometryProgram.WATER.resolveAmong(Collections.singleton("gbuffers_terrain")));
        assertEquals(
                Optional.of("gbuffers_hand"),
                GeometryProgram.HAND_WATER.resolveAmong(
                        new HashSet<>(Arrays.asList("gbuffers_hand", "gbuffers_textured_lit"))));
        assertFalse(GeometryProgram.SKYBASIC.resolveAmong(Collections.singleton("gbuffers_terrain")).isPresent());
    }

    @Test
    void translucentGeometryIsDrawnAfterTheDeferredPasses() {
        assertEquals(ProgramStage.OPAQUE_GEOMETRY, GeometryProgram.HAND.stage());
        assertEquals(ProgramStage.TRANSLUCENT_GEOMETRY, GeometryProgram.WATER.stage());
        assertEquals(Optional.of(GeometryProgram.ENTITIES_GLOWING), GeometryProgram.named("gbuffers_entities_glowing"));
        assertFalse(GeometryProgram.named("gbuffers_unicorn").isPresent());
    }

    @Test
    void fullscreenProgramsRunByStageAndThenByNumber() {
        SortedSet<FullscreenProgram> programs = new TreeSet<>();
        for (String name : Arrays.asList("final", "composite10", "composite", "deferred1", "composite2", "prepare")) {
            programs.add(FullscreenProgram.named(name).get());
        }
        List<String> order = new ArrayList<>();
        for (FullscreenProgram program : programs) {
            order.add(program.name());
        }

        assertEquals(Arrays.asList("prepare", "deferred1", "composite", "composite2", "composite10", "final"), order);
        assertTrue(ProgramStage.DEFERRED.drawsFullscreen());
        assertFalse(ProgramStage.SHADOW.drawsFullscreen());
    }

    @Test
    void aNameOutsideTheFormatIsNotAFullscreenProgram() {
        assertFalse(FullscreenProgram.named("composite100").isPresent());
        assertFalse(FullscreenProgram.named("composite0").isPresent());
        assertFalse(FullscreenProgram.named("final1").isPresent());
        assertFalse(FullscreenProgram.named("gbuffers_terrain").isPresent());
    }
}
