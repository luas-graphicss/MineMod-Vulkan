package io.github.luasgraphicss.minemod.client.lightmap;

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

class LightmapPublisherTest {
    private static final ResourceHandle TEXTURE = ResourceHandle.of(ResourceKind.TEXTURE, 1, 0);
    private static final int WRITE_TEXELS_OFFSET = 40;
    private static final byte NOON = (byte) 0xF0;
    private static final byte DUSK = (byte) 0x40;

    private final LightmapPublisher lightmap = new LightmapPublisher(TEXTURE);

    @Test
    void definesTheTextureBeforeItsFirstTexels() {
        lightmap.update(texelsOf(NOON));
        RecordedStream stream = new RecordedStream();

        lightmap.publish(stream.scene(), roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(NOON, stream.payload(1).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void writesOnlyTheTexelsOnceTheTextureIsDefined() {
        lightmap.update(texelsOf(NOON));
        lightmap.publish(new RecordedStream().scene(), roomyBudget());
        lightmap.update(texelsOf(DUSK));
        RecordedStream stream = new RecordedStream();

        lightmap.publish(stream.scene(), roomyBudget());

        assertEquals(Collections.singletonList(SceneOpcode.WRITE_TEXTURE), stream.opcodes());
        assertEquals(DUSK, stream.payload(0).get(WRITE_TEXELS_OFFSET));
    }

    @Test
    void writesNothingWhileTheTexelsAreUnchanged() {
        lightmap.update(texelsOf(NOON));
        lightmap.publish(new RecordedStream().scene(), roomyBudget());
        StreamBudget budget = roomyBudget();

        lightmap.publish(new RecordedStream().scene(), budget);

        assertFalse(budget.hasTaken());
    }

    @Test
    void keepsTexelsThatDidNotFitForTheNextStream() {
        lightmap.update(texelsOf(NOON));
        lightmap.publish(new RecordedStream().scene(), new StreamBudget(0, 0));
        RecordedStream stream = new RecordedStream();

        lightmap.publish(stream.scene(), roomyBudget());

        assertEquals(Arrays.asList(SceneOpcode.DEFINE_TEXTURE, SceneOpcode.WRITE_TEXTURE), stream.opcodes());
    }

    @Test
    void refusesTexelsOfAnotherSize() {
        assertThrows(IllegalArgumentException.class, () -> lightmap.update(ByteBuffer.allocate(16)));
    }

    private static StreamBudget roomyBudget() {
        return new StreamBudget(32 * 1024, 16);
    }

    private static ByteBuffer texelsOf(byte value) {
        byte[] texels = new byte[LightmapPublisher.TEXEL_BYTES];
        Arrays.fill(texels, value);
        return ByteBuffer.wrap(texels);
    }
}
