package io.github.luasgraphicss.minemod.compatibility.resources.framebuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.compatibility.resources.framebuffer.BufferSelection.Outcome;
import org.junit.jupiter.api.Test;

class DefaultFramebufferBuffersTest {
    private static final int GL_NONE = 0;
    private static final int GL_FRONT_LEFT = 0x0400;
    private static final int GL_FRONT_RIGHT = 0x0401;
    private static final int GL_BACK_LEFT = 0x0402;
    private static final int GL_BACK_RIGHT = 0x0403;
    private static final int GL_FRONT = 0x0404;
    private static final int GL_BACK = 0x0405;
    private static final int GL_LEFT = 0x0406;
    private static final int GL_RIGHT = 0x0407;
    private static final int GL_FRONT_AND_BACK = 0x0408;
    private static final int GL_AUX0 = 0x0409;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_COLOR_ATTACHMENT31 = 0x8CFF;
    private static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_OPERATION = 0x0502;

    private final DefaultFramebufferBuffers buffers = new DefaultFramebufferBuffers();

    @Test
    void startsDrawingToAndReadingFromTheBackBuffer() {
        assertEquals(GL_BACK, buffers.drawSelector());
        assertEquals(GL_BACK, buffers.readSelector());
    }

    @Test
    void theBackBufferIsTheSceneColorAttachment() {
        for (int selector : new int[] {GL_BACK, GL_BACK_LEFT}) {
            assertEquals(GL_COLOR_ATTACHMENT0, buffers.selectDraw(selector).providerBuffer());
            assertEquals(GL_COLOR_ATTACHMENT0, buffers.selectRead(selector).providerBuffer());
        }
    }

    @Test
    void selectingNoBufferIsForwardedAsNone() {
        assertEquals(Outcome.NO_BUFFER, buffers.selectDraw(GL_NONE).outcome());
        assertEquals(GL_NONE, buffers.selectRead(GL_NONE).providerBuffer());
    }

    @Test
    void remembersTheSelectorTheCallerUsed() {
        buffers.selectDraw(GL_BACK_LEFT);
        buffers.selectRead(GL_NONE);

        assertEquals(GL_BACK_LEFT, buffers.drawSelector());
        assertEquals(GL_NONE, buffers.readSelector());
    }

    @Test
    void anySelectionThatReachesTheFrontBufferIsUnavailable() {
        for (int selector : new int[] {GL_FRONT, GL_FRONT_LEFT, GL_LEFT, GL_FRONT_AND_BACK}) {
            assertEquals(Outcome.FRONT_BUFFER_UNAVAILABLE, buffers.selectDraw(selector).outcome());
            assertEquals(Outcome.FRONT_BUFFER_UNAVAILABLE, buffers.selectRead(selector).outcome());
        }
        assertEquals(GL_BACK, buffers.drawSelector());
        assertEquals(GL_BACK, buffers.readSelector());
    }

    @Test
    void buffersTheDefaultFramebufferLacksAreInvalidOperations() {
        int[] absent = {GL_RIGHT, GL_FRONT_RIGHT, GL_BACK_RIGHT, GL_AUX0, GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT31};
        for (int selector : absent) {
            assertEquals(GL_INVALID_OPERATION, buffers.selectDraw(selector).error());
            assertEquals(GL_INVALID_OPERATION, buffers.selectRead(selector).error());
        }
    }

    @Test
    void valuesThatNameNoBufferAreInvalidEnums() {
        for (int selector : new int[] {GL_TEXTURE_2D, GL_DEPTH_ATTACHMENT, -1}) {
            assertEquals(GL_INVALID_ENUM, buffers.selectDraw(selector).error());
            assertEquals(GL_INVALID_ENUM, buffers.selectRead(selector).error());
        }
    }

    @Test
    void aRejectedSelectionKeepsThePreviousSelector() {
        buffers.selectDraw(GL_NONE);

        BufferSelection rejected = buffers.selectDraw(GL_BACK_RIGHT);

        assertFalse(rejected.isApplied());
        assertEquals(GL_NONE, buffers.drawSelector());
        assertThrows(IllegalStateException.class, rejected::providerBuffer);
        assertThrows(IllegalStateException.class, buffers.selectRead(GL_BACK)::error);
    }
}
