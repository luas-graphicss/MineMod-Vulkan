package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentOwner;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulatedProviderTest {
    private static final long GL_TEXTURE_2D = 0x0DE1;
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_PACK_ALIGNMENT = 0x0D05;
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final long GL_RGB = 0x1907;
    private static final long GL_COMPILE_STATUS = 0x8B81;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_OPERATION = 0x0502;

    private final Timeline timeline = new Timeline();
    private AttachmentOwner owner = AttachmentOwner.PROVIDER;

    @Test
    void objectsAreShownByKindAndOrderOfCreationAndNamesAreReused() {
        SimulatedProvider provider = provider(ContextProfile.CORE);
        int first = provider.create(ObjectKind.TEXTURE, 0);
        int second = provider.create(ObjectKind.TEXTURE, 0);
        provider.dispose(ObjectKind.TEXTURE, first);

        assertEquals(first, provider.create(ObjectKind.TEXTURE, 0));
        provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, second});
        provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, first});

        assertEquals(
                List.of("glBindTexture(3553, TEXTURE#2)", "glBindTexture(3553, TEXTURE#3)"),
                timeline.from(Timeline.Source.PROVIDER));
    }

    @Test
    void aCoreContextRejectsAnUnknownNameAndACompatibilityContextCreatesIt() {
        SimulatedProvider core = provider(ContextProfile.CORE);
        core.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, 9});
        assertEquals(GL_INVALID_OPERATION, core.takeError());
        assertEquals(0, timeline.size());

        SimulatedProvider compatibility = provider(ContextProfile.COMPATIBILITY);
        compatibility.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, 9});
        assertTrue(compatibility.isLive(ObjectKind.TEXTURE, 9));
        assertEquals(SimulatedProvider.NO_ERROR, compatibility.takeError());
    }

    @Test
    void theFirstErrorIsKeptUntilItIsTaken() {
        SimulatedProvider provider = provider(ContextProfile.CORE);

        provider.invoke("glEnable", new long[] {SimulatedProvider.INVALID_CAPABILITY});
        provider.invoke("glBindVertexArray", new long[] {4});

        assertEquals(GL_INVALID_ENUM, provider.takeError());
        assertEquals(SimulatedProvider.NO_ERROR, provider.takeError());
    }

    @Test
    void aDrawWhileTheRendererOwnsTheAttachmentsIsRecordedAsAViolation() {
        SimulatedProvider provider = provider(ContextProfile.CORE);
        provider.invoke("glDrawArrays", new long[] {GL_TRIANGLES, 0, 3});
        assertEquals(List.of(), provider.ownershipViolations());

        owner = AttachmentOwner.RENDERER;
        provider.invoke("glDrawArrays", new long[] {GL_TRIANGLES, 0, 3});

        assertEquals(1, provider.ownershipViolations().size());
    }

    @Test
    void displayListsComeInContiguousRunsAndUndefinedOnesAreSkipped() {
        SimulatedProvider provider = provider(ContextProfile.COMPATIBILITY);
        provider.create(ObjectKind.DISPLAY_LIST, 0);
        int single = provider.create(ObjectKind.DISPLAY_LIST, 0);
        provider.dispose(ObjectKind.DISPLAY_LIST, 1);

        assertEquals(single + 1, provider.generate(2));
        provider.invoke("glCallLists", new long[] {single, 1});

        assertEquals(List.of("glCallLists(DISPLAY_LIST#2, none)"), timeline.from(Timeline.Source.PROVIDER));
        assertFalse(provider.isLive(ObjectKind.DISPLAY_LIST, 1));
    }

    @Test
    void aReadbackPadsItsRowsToThePackAlignment() {
        SimulatedProvider provider = provider(ContextProfile.CORE);
        long[] threeByTwoRgb = {0, 0, 3, 2, GL_RGB, GL_UNSIGNED_BYTE, 0};

        assertEquals(24, provider.invoke("glReadPixels", threeByTwoRgb));
        provider.invoke("glPixelStorei", new long[] {GL_PACK_ALIGNMENT, 1});
        assertEquals(18, provider.invoke("glReadPixels", threeByTwoRgb));
    }

    @Test
    void scriptedAnswersAreGivenInOrderUntilTheyRunOut() {
        SimulatedProvider provider = provider(ContextProfile.CORE);
        int shader = provider.create(ObjectKind.SHADER_OR_PROGRAM, 0);
        provider.answer("glGetShaderiv", 1, 57);

        assertEquals(1, provider.invoke("glGetShaderiv", new long[] {shader, GL_COMPILE_STATUS, 0}));
        assertEquals(57, provider.invoke("glGetShaderiv", new long[] {shader, GL_COMPILE_STATUS, 0}));
        assertEquals(0, provider.invoke("glGetShaderiv", new long[] {shader, GL_COMPILE_STATUS, 0}));
    }

    private SimulatedProvider provider(ContextProfile profile) {
        return new SimulatedProvider(profile, timeline, () -> owner);
    }
}
