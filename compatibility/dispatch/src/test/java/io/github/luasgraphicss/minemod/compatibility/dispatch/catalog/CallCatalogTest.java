package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class CallCatalogTest {
    private final CallCatalog catalog = CallCatalog.standard();

    @Test
    void everyFunctionOfAnApprovedExtensionIsDescribed() {
        for (String extension : catalog.extensions().extensions()) {
            for (String function : catalog.extensions().functionsOf(extension)) {
                assertTrue(catalog.describe(function).isPresent(), extension + " lists " + function);
            }
        }
    }

    @Test
    void legacyFamiliesAreToldApartFromTheModernOnes() {
        assertTrue(catalog.describe("glBegin").get().family().isLegacy());
        assertTrue(catalog.describe("glPushMatrix").get().family().isLegacy());
        assertTrue(catalog.describe("glCallList").get().family().isLegacy());
        assertFalse(catalog.describe("glDrawElements").get().family().isLegacy());
        assertFalse(catalog.describe("glBindFramebuffer").get().family().isLegacy());
    }

    @Test
    void anExtensionEntryPointSharesTheDescriptionOfItsCoreFunction() {
        CallDescription description = catalog.describe("glBindBufferARB").get();

        assertEquals("glBindBuffer", description.function());
        assertEquals(ObjectKind.BUFFER, description.nameArguments().get(0).kind());
        assertEquals(1, description.nameArguments().get(0).index());
    }

    @Test
    void aSuffixedFunctionOutsideTheApprovedExtensionsStaysUndescribed() {
        assertFalse(catalog.describe("glBindTextureEXT").isPresent());
        assertFalse(catalog.describe("glUniformBufferEXT").isPresent());
        assertFalse(catalog.describe("glProgramStringARB").isPresent());
    }

    @Test
    void drawsAndReadsCarryTheirCheckpoint() {
        assertEquals(Checkpoint.DRAW, catalog.describe("glDrawArrays").get().checkpoint());
        assertEquals(Checkpoint.DRAW, catalog.describe("glBlitFramebuffer").get().checkpoint());
        assertEquals(Checkpoint.READ, catalog.describe("glReadPixels").get().checkpoint());
        assertEquals(Checkpoint.NONE, catalog.describe("glBindTexture").get().checkpoint());
    }

    @Test
    void callsThatReturnOrConsumeStateAreNeverForwardedUntouched() {
        assertEquals(Interception.ERRORS, catalog.describe("glGetError").get().interception());
        assertEquals(Interception.CAPABILITIES, catalog.describe("glGetStringi").get().interception());
        assertEquals(Interception.STATE_QUERY, catalog.describe("glGetIntegerv").get().interception());
        assertEquals(Interception.SPANS, catalog.describe("glBeginTransformFeedback").get().interception());
        assertEquals(Interception.FRAMEBUFFER, catalog.describe("glFramebufferTexture2D").get().interception());
    }

    @Test
    void aFunctionCannotBeDescribedTwice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CallCatalog(
                        CallCatalogParser.parse(Arrays.asList(
                                "glFlush SUBMISSION FORWARD NONE", "gl{Flush,Finish} SUBMISSION FORWARD NONE")),
                        new ExtensionCatalog(Collections.<String>emptyList())));
    }
}
