package io.github.luasgraphicss.minemod.compatibility.translation.framebuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallCatalog;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.DefaultFramebufferBuffers;
import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.FramebufferBindings;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.support.LayerFixture;
import io.github.luasgraphicss.minemod.compatibility.translation.support.RecordingProvider;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class VirtualFramebufferCallsTest {
    private static final int SCENE = 7;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_BACK = 0x0405;
    private static final int GL_FRONT = 0x0404;
    private static final int GL_BACK_LEFT = 0x0402;
    private static final int GL_DEPTH = 0x1801;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_COLOR_ATTACHMENT1 = 0x8CE1;
    private static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_OPERATION = 0x0502;

    private final LayerFixture fixture = new LayerFixture();
    private final ObjectNames names = fixture.names(ContextProfile.CORE);
    private final FramebufferBindings bindings =
            new FramebufferBindings(fixture.contextNames.table(ObjectKind.FRAMEBUFFER), SCENE);
    private final VirtualFramebufferCalls calls = new VirtualFramebufferCalls(
            bindings,
            new DefaultFramebufferBuffers(),
            names,
            CallCatalog.standard(),
            fixture.provider,
            fixture.errors,
            fixture.reporter);

    @Test
    void theDefaultFramebufferIsTheSceneAndNotTheProvidersOwn() {
        calls.bind(GL_FRAMEBUFFER, 0);

        assertEquals(Collections.singletonList("glBindFramebuffer[36160, 7]"), fixture.provider.events());
    }

    @Test
    void readAndDrawBindingsMoveOnTheirOwn() {
        int offscreen = names.generate(ObjectKind.FRAMEBUFFER, 1)[0];

        calls.bind(GL_DRAW_FRAMEBUFFER, offscreen);

        assertTrue(bindings.readsFromDefault());
        assertFalse(bindings.drawsToDefault());
        assertEquals(
                Collections.singletonList("glBindFramebuffer[36009, " + RecordingProvider.FIRST_BACKING + "]"),
                fixture.provider.events());
    }

    @Test
    void theBackBufferOfTheDefaultFramebufferIsTheSceneColour() {
        calls.drawBuffer(GL_BACK);
        calls.readBuffer(GL_BACK);

        assertEquals(Arrays.asList("glDrawBuffer[36064]", "glReadBuffer[36064]"), fixture.provider.events());
    }

    @Test
    void theFrontBufferIsReportedInsteadOfPretended() {
        calls.readBuffer(GL_FRONT);

        assertEquals(Collections.emptyList(), fixture.provider.events());
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
        assertEquals("glReadBuffer on the front buffer", fixture.onlyReport().operation());
        assertEquals(OperationSupport.OUT_OF_CONTRACT, fixture.onlyReport().support());
    }

    @Test
    void aUserFramebufferKeepsItsOwnBufferSelection() {
        calls.bind(GL_FRAMEBUFFER, names.generate(ObjectKind.FRAMEBUFFER, 1)[0]);

        calls.drawBuffer(GL_COLOR_ATTACHMENT1);
        assertFalse(calls.drawBuffersOnDefault(new int[] {GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1}));

        assertEquals("glDrawBuffer[36065]", fixture.provider.events().get(1));
    }

    @Test
    void severalDrawBuffersOnTheDefaultFramebufferAreAnError() {
        assertTrue(calls.drawBuffersOnDefault(new int[] {GL_BACK, GL_BACK}));
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());

        assertTrue(calls.drawBuffersOnDefault(new int[] {GL_BACK}));
        assertEquals(Collections.singletonList("glDrawBuffer[36064]"), fixture.provider.events());
    }

    @Test
    void deletingTheBoundFramebufferFallsBackToTheScene() {
        int offscreen = names.generate(ObjectKind.FRAMEBUFFER, 1)[0];
        calls.bind(GL_FRAMEBUFFER, offscreen);

        calls.delete(new int[] {offscreen});

        assertTrue(bindings.drawsToDefault());
        assertTrue(bindings.readsFromDefault());
        assertEquals(
                Arrays.asList("glBindFramebuffer[36008, 7]", "glBindFramebuffer[36009, 7]"),
                fixture.provider.events().subList(1, 3));
        assertFalse(names.isLive(ObjectKind.FRAMEBUFFER, offscreen));
    }

    @Test
    void anAttachmentNeverReachesTheSceneThroughTheDefaultFramebuffer() {
        int texture = names.generate(ObjectKind.TEXTURE, 1)[0];

        calls.attach("glFramebufferTexture2D", new long[] {GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0});

        assertEquals(Collections.emptyList(), fixture.provider.events());
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
    }

    @Test
    void anAttachmentToAUserFramebufferCarriesTheProvidersTexture() {
        int texture = names.generate(ObjectKind.TEXTURE, 1)[0];
        calls.bind(GL_FRAMEBUFFER, names.generate(ObjectKind.FRAMEBUFFER, 1)[0]);

        calls.attach("glFramebufferTexture2DEXT", new long[] {GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0});

        assertEquals(
                "glFramebufferTexture2DEXT[36160, 36064, 3553, " + RecordingProvider.FIRST_BACKING + ", 0]",
                fixture.provider.events().get(1));
    }

    @Test
    void theAttachmentsOfTheDefaultFramebufferMapOntoTheScene() {
        assertEquals(GL_COLOR_ATTACHMENT0, calls.providerAttachment(GL_READ_FRAMEBUFFER, GL_BACK_LEFT));
        assertEquals(GL_DEPTH_ATTACHMENT, calls.providerAttachment(GL_FRAMEBUFFER, GL_DEPTH));
        assertEquals(VirtualFramebufferCalls.REJECTED, calls.providerAttachment(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0));
        assertEquals(GL_INVALID_ENUM, fixture.errors.take());
    }

    @Test
    void anUnknownTargetOrNameIsAnErrorAndBindsNothing() {
        calls.bind(GL_TEXTURE_2D, 0);
        assertEquals(GL_INVALID_ENUM, fixture.errors.take());

        calls.bind(GL_FRAMEBUFFER, 5);
        assertEquals(GL_INVALID_OPERATION, fixture.errors.take());
        assertEquals(Collections.emptyList(), fixture.provider.events());
    }
}
