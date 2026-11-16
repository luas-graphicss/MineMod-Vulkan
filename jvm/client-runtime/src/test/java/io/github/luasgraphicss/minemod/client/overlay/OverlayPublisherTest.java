package io.github.luasgraphicss.minemod.client.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.client.frame.StreamBudget;
import io.github.luasgraphicss.minemod.client.support.RecordedStream;
import io.github.luasgraphicss.minemod.codec.scene.SceneOpcode;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class OverlayPublisherTest {
    private static final ResourceHandle TEXTURE = ResourceHandle.of(ResourceKind.TEXTURE, 1, 0);
    private static final int WRITE_TEXELS_OFFSET = 40;
    private static final byte NOON = (byte) 0xF0;
    private static final byte DUSK = (byte) 0x40;

    private final OverlayPublisher overlay = new OverlayPublisher(TEXTURE);

    @Test
    void definesTheTextureBeforeItsFirstTexels() {
        overlay.update(texelsOf(NOON));
        RecordedStream stream = new RecordedStream();

        overlay.publish(stream.scene(), roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(NOON, stream.payload(1).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void writesOnlyTheTexelsOnceTheTextureIsDefined() {
        overlay.update(texelsOf(NOON));
        overlay.publish(new RecordedStream().scene(), roomyBudget());
        overlay.update(texelsOf(DUSK));
        RecordedStream stream = new RecordedStream();

        overlay.publish(stream.scene(), roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(DUSK, stream.payload(0).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void writesNothingWhileTheTexelsAreUnchanged() {
        overlay.update(texelsOf(NOON));
        overlay.publish(new RecordedStream().scene(), roomyBudget());
        StreamBudget budget = roomyBudget();

        overlay.publish(new RecordedStream().scene(), budget);

        assertFalse(budget.hasTaken());
    }

    @Test
    void keepsTexelsThatDidNotFitForTheNextStream() {
        overlay.update(texelsOf(NOON));
        overlay.publish(new RecordedStream().scene(), new StreamBudget(0, 0));
        RecordedStream stream = new RecordedStream();

        overlay.publish(stream.scene(), roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
    }

    @Test
    void refusesTexelsOfAnotherSize() {
        assertThrows(IllegalArgumentException.class, () -> overlay.update(ByteBuffer.allocate(16)));
    }

    private static StreamBudget roomyBudget() {
        return new StreamBudget(32 * 1024, 16);
    }

    private static ByteBuffer texelsOf(byte value) {
        byte[] texels = new byte[OverlayPublisher.TEXEL_BYTES];
        Arrays.fill(texels, value);
        return ByteBuffer.wrap(texels);
    }
}
