package io.github.luasgraphicss.minemod.compatibility.translation.names;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ObjectNamesTest {
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int GL_OUT_OF_MEMORY = 0x0505;

    private final LayerFixture fixture = new LayerFixture();
    private final ObjectNames core = fixture.names(ContextProfile.CORE);
    private final ObjectNames compatibility = fixture.names(ContextProfile.COMPATIBILITY);

    @Test
    void generatedNamesBelongToTheModAndStandForProviderObjects() {
        assertArrayEquals(new int[] {1, 2}, core.generate(ObjectKind.TEXTURE, 2));

        assertEquals(RecordingProvider.FIRST_BACKING, core.backingForUse(ObjectKind.TEXTURE, 1));
        assertEquals(RecordingProvider.FIRST_BACKING + 1, core.backingForUse(ObjectKind.TEXTURE, 2));
        assertEquals(2, core.nameOfBacking(ObjectKind.TEXTURE, RecordingProvider.FIRST_BACKING + 1));
        assertEquals(0, core.nameOfBacking(ObjectKind.TEXTURE, 0));
    }

    @Test
    void nameZeroIsNeverTranslated() {
        assertEquals(0, core.backingForUse(ObjectKind.BUFFER, 0));
        assertEquals(0, fixture.errors.take());
    }

    @Test
    void anObjectStillUsedByAFrameInFlightOutlivesItsDeletion() {
        int[] names = core.generate(ObjectKind.BUFFER, 2);
        fixture.frame = 5;
        core.backingForUse(ObjectKind.BUFFER, names[0]);

        core.delete(ObjectKind.BUFFER, new int[] {names[0], names[1], 0, 99});

        assertFalse(core.isLive(ObjectKind.BUFFER, names[0]));
        assertEquals(Collections.singletonList("BUFFER 101"), fixture.provider.disposed());
        fixture.contextNames.table(ObjectKind.BUFFER).collect(5, fixture.provider);
        assertEquals(2, fixture.provider.disposed().size());
    }

    @Test
    void aCompatibilityContextCreatesATextureWhenItIsFirstBound() {
        assertEquals(RecordingProvider.FIRST_BACKING, compatibility.backingForUse(ObjectKind.TEXTURE, 9));

        assertTrue(compatibility.isLive(ObjectKind.TEXTURE, 9));
        assertEquals(0, fixture.errors.take());
    }

    @Test
    void aCompatibilityContextCreatesARenderbufferOrAFramebufferWhenItIsFirstBound() {
        compatibility.backingForUse(ObjectKind.RENDERBUFFER, 7);
        compatibility.backingForUse(ObjectKind.FRAMEBUFFER, 7);

        assertTrue(compatibility.isLive(ObjectKind.RENDERBUFFER, 7));
        assertTrue(compatibility.isLive(ObjectKind.FRAMEBUFFER, 7));
        assertEquals(0, fixture.errors.take());
    }

    @Test
    void aNameThatIsNoShaderOrProgramIsAnInvalidValueWhenUsedOrDeleted() {
        assertEquals(ObjectNames.REJECTED, core.backingForUse(ObjectKind.SHADER_OR_PROGRAM, 9));
        assertEquals(GL_INVALID_VALUE, fixture.errors.take());

        core.delete(ObjectKind.SHADER_OR_PROGRAM, new int[] {9});
        assertEquals(GL_INVALID_VALUE, fixture.errors.take());
    }

    @Test
    void deletingNameZeroOrAnUnusedNameOfAnotherKindRaisesNothing() {
        core.delete(ObjectKind.SHADER_OR_PROGRAM, new int[] {0});
        core.delete(ObjectKind.TEXTURE, new int[] {9});

        assertEquals(0, fixture.errors.take());
    }

    @Test
    void aCoreContextRejectsANameItNeverGenerated() {
        assertEquals(ObjectNames.REJECTED, core.backingForUse(ObjectKind.TEXTURE, 9));

        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
        assertFalse(core.isLive(ObjectKind.TEXTURE, 9));
    }

    @Test
    void vertexArraysAreNeverCreatedByBindingThem() {
        assertEquals(ObjectNames.REJECTED, compatibility.backingForUse(ObjectKind.VERTEX_ARRAY, 3));
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
    }

    @Test
    void aFullTableReportsOutOfMemoryAndKeepsNoProviderObject() {
        core.generate(ObjectKind.SAMPLER, LayerFixture.NAMES_PER_KIND);

        assertEquals(0, core.create(ObjectKind.SAMPLER, 0));

        assertEquals(GL_OUT_OF_MEMORY, fixture.errors.take());
        assertEquals(
                Collections.singletonList("SAMPLER " + (RecordingProvider.FIRST_BACKING + LayerFixture.NAMES_PER_KIND)),
                fixture.provider.disposed());
        assertEquals(ObjectNames.REJECTED, compatibility.backingForUse(ObjectKind.TEXTURE, 4096));
        assertEquals(GL_OUT_OF_MEMORY, fixture.errors.take());
    }

    @Test
    void aNegativeCountIsAnInvalidValue() {
        assertEquals(0, core.generate(ObjectKind.QUERY, -1).length);
        assertEquals(GL_INVALID_VALUE, fixture.errors.take());
    }
}
