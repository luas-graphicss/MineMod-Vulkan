package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SamplerNamesTest {
    @Test
    void namesTheTargetOfNumberedAndLegacySamplers() {
        assertEquals(Optional.of(PackTarget.color(15)), SamplerNames.targetOf("colortex15", false));
        assertEquals(Optional.of(PackTarget.color(3)), SamplerNames.targetOf("composite", false));
        assertEquals(Optional.of(PackTarget.color(7)), SamplerNames.targetOf("gaux4", false));
        assertEquals(Optional.of(PackTarget.depth(0)), SamplerNames.targetOf("gdepthtex", false));
        assertEquals(Optional.of(PackTarget.depth(2)), SamplerNames.targetOf("depthtex2", false));
        assertEquals(Optional.of(PackTarget.shadowColor(0)), SamplerNames.targetOf("shadowcolor", false));
    }

    @Test
    void theLegacyShadowSamplerMovesWhenTheProgramDeclaresWaterShadow() {
        assertEquals(Optional.of(PackTarget.shadowDepth(0)), SamplerNames.targetOf("shadow", false));
        assertEquals(Optional.of(PackTarget.shadowDepth(1)), SamplerNames.targetOf("shadow", true));
        assertEquals(Optional.of(PackTarget.shadowDepth(0)), SamplerNames.targetOf("watershadow", true));
    }

    @Test
    void aSamplerBeyondTheTargetsOfAPackNamesNothing() {
        assertFalse(SamplerNames.targetOf("colortex16", false).isPresent());
        assertFalse(SamplerNames.targetOf("depthtex3", false).isPresent());
        assertFalse(SamplerNames.targetOf("bloomtex", false).isPresent());
        assertEquals(OptionalInt.empty(), SamplerNames.colorIndexOf("depthtex0"));
        assertThrows(IllegalArgumentException.class, () -> PackTarget.shadowDepth(2));
    }

    @Test
    void theTexturesOfTheGameAreNotTargetsOfThePack() {
        assertTrue(SamplerNames.boundByTheGame("lightmap"));
        assertTrue(SamplerNames.boundByTheGame("gtexture"));
        assertFalse(SamplerNames.boundByTheGame("colortex0"));
        assertEquals("colortex4.ALT", new TargetBinding(PackTarget.color(4), BufferSide.ALT).toString());
        assertEquals("depthtex1", new TargetBinding(PackTarget.depth(1), BufferSide.MAIN).toString());
    }
}
