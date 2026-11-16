package io.github.luasgraphicss.minemod.client.shaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.codec.scene.BatchState;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ProgramCatalogTest {
    private final ProgramCatalog catalog = new ProgramCatalog();

    @Test
    void drawsAShaderNoPackReplacesWithTheBuiltInProgram() {
        catalog.idOf("rendertype_solid");

        assertEquals(BatchState.BUILT_IN_PROGRAM, catalog.programOf("rendertype_solid"));
        assertEquals(BatchState.BUILT_IN_PROGRAM, catalog.programOf("position_tex"));
        assertEquals(0, catalog.publishedCount());
    }

    @Test
    void drawsAPublishedShaderWithItsProgram() {
        int solid = catalog.idOf("rendertype_solid");

        catalog.publish(Collections.singleton("rendertype_solid"));

        assertEquals(solid, catalog.programOf("rendertype_solid"));
        assertEquals(1, catalog.publishedCount());
    }

    @Test
    void keepsTheIdOfAShaderAcrossGenerations() {
        int solid = catalog.idOf("rendertype_solid");
        int text = catalog.idOf("rendertype_text");
        catalog.publish(Collections.singleton("rendertype_solid"));
        catalog.publish(Collections.<String>emptySet());

        assertEquals(solid, catalog.idOf("rendertype_solid"));
        assertEquals(text, catalog.idOf("rendertype_text"));
        assertEquals(BatchState.BUILT_IN_PROGRAM, catalog.programOf("rendertype_solid"));
    }
}
