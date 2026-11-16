package io.github.luasgraphicss.minemod.client.terrain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.client.support.RecordedStream;
import io.github.luasgraphicss.minemod.codec.scene.SectionMesh;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class SectionMeshAssemblerTest {
    private static final int COUNTS_OFFSET = 16;
    private static final int RANGES_OFFSET = 24;
    private static final int BLOB_OFFSET = 64;

    @Test
    void keepsPositionColorTextureAndLightOfEveryVertex() {
        ByteBuffer published = publish(TerrainFixtures.solidQuads(1));

        assertEquals(TerrainFixtures.QUAD_VERTICES, published.getInt(COUNTS_OFFSET));
        for (int vertex = 0; vertex < TerrainFixtures.QUAD_VERTICES; vertex++) {
            for (int offset = 0; offset < SectionMesh.VERTEX_BYTES; offset++) {
                assertEquals(
                        TerrainFixtures.keptByte(vertex, offset),
                        published.get(BLOB_OFFSET + vertex * SectionMesh.VERTEX_BYTES + offset));
            }
        }
    }

    @Test
    void drawsQuadsInBuildOrderAsTwoTrianglesEach() {
        ByteBuffer published = publish(TerrainFixtures.solidQuads(2));

        assertArrayEquals(new int[] {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4}, indices(published));
    }

    @Test
    void keepsTheDrawOrderTheGameSortedAndRebasesItOnTheLayer() {
        ByteBuffer sorted = ByteBuffer.allocate(2 * TerrainFixtures.QUAD_INDICES * Short.BYTES)
                .order(ByteOrder.nativeOrder());
        for (int index : new int[] {4, 5, 6, 6, 7, 4, 0, 1, 2, 2, 3, 0}) {
            sorted.putShort((short) index);
        }
        sorted.flip();
        SectionGeometry geometry = new SectionMeshAssembler()
                .add(
                        SectionLayer.TRANSLUCENT,
                        LayerQuads.inDrawOrder(
                                TerrainFixtures.gameQuads(2), TerrainFixtures.GAME_VERTEX_BYTES, sorted, Short.BYTES))
                .add(
                        SectionLayer.SOLID,
                        LayerQuads.inBuildOrder(TerrainFixtures.gameQuads(1), TerrainFixtures.GAME_VERTEX_BYTES))
                .assemble();

        ByteBuffer published = publish(geometry);

        assertArrayEquals(
                new int[] {0, 1, 2, 2, 3, 0, 8, 9, 10, 10, 11, 8, 4, 5, 6, 6, 7, 4}, indices(published));
        assertArrayEquals(new int[] {0, 6, 6, 0, 6, 0, 6, 12}, layerRanges(published));
    }

    @Test
    void drawsLaterQuadsOfALayerAfterItsEarlierOnes() {
        SectionGeometry geometry = new SectionMeshAssembler()
                .add(
                        SectionLayer.TRANSLUCENT,
                        LayerQuads.inBuildOrder(TerrainFixtures.gameQuads(1), TerrainFixtures.GAME_VERTEX_BYTES))
                .add(
                        SectionLayer.TRANSLUCENT,
                        LayerQuads.inBuildOrder(TerrainFixtures.gameQuads(1), TerrainFixtures.GAME_VERTEX_BYTES))
                .assemble();

        ByteBuffer published = publish(geometry);

        assertArrayEquals(new int[] {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4}, indices(published));
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 12}, layerRanges(published));
    }

    @Test
    void reportsASectionWithoutQuadsAsEmpty() {
        assertTrue(new SectionMeshAssembler().assemble().isEmpty());
        assertFalse(TerrainFixtures.solidQuads(1).isEmpty());
    }

    @Test
    void refusesVerticesShorterThanTheTerrainLayout() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LayerQuads.inBuildOrder(ByteBuffer.allocate(96), SectionMesh.VERTEX_BYTES - 4));
    }

    @Test
    void refusesADrawOrderThatNamesAMissingVertex() {
        ByteBuffer order = ByteBuffer.allocate(TerrainFixtures.QUAD_INDICES * Integer.BYTES)
                .order(ByteOrder.nativeOrder());
        for (int index : new int[] {0, 1, 2, 2, 3, 4}) {
            order.putInt(index);
        }
        order.flip();
        SectionMeshAssembler assembler = new SectionMeshAssembler()
                .add(
                        SectionLayer.TRANSLUCENT,
                        LayerQuads.inDrawOrder(
                                TerrainFixtures.gameQuads(1), TerrainFixtures.GAME_VERTEX_BYTES, order, Integer.BYTES));

        assertThrows(IllegalArgumentException.class, assembler::assemble);
    }

    @Test
    void refusesADrawOrderThatDoesNotCoverTheQuads() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LayerQuads.inDrawOrder(
                        TerrainFixtures.gameQuads(1),
                        TerrainFixtures.GAME_VERTEX_BYTES,
                        ByteBuffer.allocate(Short.BYTES),
                        Short.BYTES));
    }

    private static ByteBuffer publish(SectionGeometry geometry) {
        RecordedStream stream = new RecordedStream();
        stream.scene().publishSection(0, 0, 0, 0, geometry.mesh());
        stream.opcodes();
        return stream.payload(0);
    }

    private static int[] indices(ByteBuffer published) {
        int vertexCount = published.getInt(COUNTS_OFFSET);
        int[] indices = new int[published.getInt(COUNTS_OFFSET + Integer.BYTES)];
        for (int index = 0; index < indices.length; index++) {
            indices[index] =
                    published.getInt(BLOB_OFFSET + vertexCount * SectionMesh.VERTEX_BYTES + index * Integer.BYTES);
        }
        return indices;
    }

    private static int[] layerRanges(ByteBuffer published) {
        int[] ranges = new int[SectionMesh.LAYER_COUNT * 2];
        for (int value = 0; value < ranges.length; value++) {
            ranges[value] = published.getInt(RANGES_OFFSET + value * Integer.BYTES);
        }
        return ranges;
    }
}
