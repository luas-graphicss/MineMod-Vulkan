package io.github.luasgraphicss.minemod.client.draw.vertex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class VertexRepackerTest {
    private static final int COLOR = 0x11223344;
    private static final int LIGHT = 0x00F000F0;
    private static final GameVertexLayout POSITION_COLOR = GameVertexLayout.builder(16)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.COLOR, 12)
            .build();
    private static final GameVertexLayout PARTICLE = GameVertexLayout.builder(28)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.TEXTURE, 12)
            .element(VertexElement.COLOR, 20)
            .element(VertexElement.LIGHT, 24)
            .build();
    private static final GameVertexLayout PADDED = GameVertexLayout.builder(20)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.COLOR, 12)
            .build();

    @Test
    void aLayoutInTheOrderOfTheSceneIsCopiedAsItIs() {
        ByteBuffer vertices = buffer(32);
        for (int value = 0; value < 8; value++) {
            vertices.putInt(value);
        }
        vertices.flip();
        ByteBuffer packed = buffer(32);

        VertexRepacker.pack(POSITION_COLOR, vertices, 2, packed);

        assertTrue(POSITION_COLOR.isPacked());
        assertEquals(32, packed.position());
        assertEquals(0, vertices.position());
        assertEquals(7, packed.getInt(28));
    }

    @Test
    void elementsInAnotherOrderAreMovedToTheOrderOfTheScene() {
        ByteBuffer vertices = buffer(28);
        vertices.putFloat(1.0f).putFloat(2.0f).putFloat(3.0f).putFloat(0.25f).putFloat(0.75f).putInt(COLOR).putInt(LIGHT);
        vertices.flip();
        ByteBuffer packed = buffer(28);

        VertexRepacker.pack(PARTICLE, vertices, 1, packed);

        assertFalse(PARTICLE.isPacked());
        assertEquals(3.0f, packed.getFloat(8));
        assertEquals(COLOR, packed.getInt(12));
        assertEquals(0.25f, packed.getFloat(16));
        assertEquals(0.75f, packed.getFloat(20));
        assertEquals(LIGHT, packed.getInt(24));
    }

    @Test
    void bytesThatBelongToNoElementAreLeftBehind() {
        ByteBuffer vertices = buffer(40);
        for (int vertex = 0; vertex < 2; vertex++) {
            vertices.putFloat(vertex).putFloat(0.0f).putFloat(0.0f).putInt(COLOR + vertex).putInt(-1);
        }
        vertices.flip();
        ByteBuffer packed = buffer(32);

        VertexRepacker.pack(PADDED, vertices, 2, packed);

        assertEquals(16, PADDED.packedStride());
        assertEquals(32, packed.position());
        assertEquals(1.0f, packed.getFloat(16));
        assertEquals(COLOR + 1, packed.getInt(28));
    }

    @Test
    void packingStartsWhereTheVerticesAreRead() {
        ByteBuffer vertices = buffer(48);
        for (int value = 0; value < 12; value++) {
            vertices.putInt(value);
        }
        vertices.flip().position(16);
        ByteBuffer packed = buffer(16);

        VertexRepacker.pack(POSITION_COLOR, vertices, 1, packed);

        assertEquals(4, packed.getInt(0));
    }

    @Test
    void moreVerticesThanTheBytesHoldAreRefused() {
        ByteBuffer vertices = buffer(16);

        assertThrows(IllegalArgumentException.class, () -> VertexRepacker.pack(POSITION_COLOR, vertices, 2, buffer(32)));
    }

    @Test
    void aLayoutNeedsAPositionAndElementsInsideTheVertex() {
        assertThrows(IllegalArgumentException.class, () -> GameVertexLayout.builder(4)
                .element(VertexElement.COLOR, 0)
                .build());
        assertThrows(IllegalArgumentException.class, () -> GameVertexLayout.builder(12)
                .element(VertexElement.POSITION, 4));
        assertThrows(IllegalArgumentException.class, () -> GameVertexLayout.builder(24)
                .element(VertexElement.POSITION, 0)
                .element(VertexElement.POSITION, 12));
    }

    private static ByteBuffer buffer(int bytes) {
        return ByteBuffer.allocate(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }
}
