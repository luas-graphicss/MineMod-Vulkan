package io.github.luasgraphicss.minemod.client.terrain;

import io.github.luasgraphicss.minemod.codec.scene.SectionMesh;
import java.nio.ByteBuffer;

final class TerrainFixtures {
    static final int GAME_VERTEX_BYTES = 32;
    static final int QUAD_VERTICES = 4;
    static final int QUAD_INDICES = 6;
    static final byte NORMAL_BYTE = (byte) 0xEE;

    private TerrainFixtures() {}

    static ByteBuffer gameQuads(int quadCount) {
        ByteBuffer vertices = ByteBuffer.allocate(quadCount * QUAD_VERTICES * GAME_VERTEX_BYTES);
        for (int vertex = 0; vertex < quadCount * QUAD_VERTICES; vertex++) {
            for (int offset = 0; offset < SectionMesh.VERTEX_BYTES; offset++) {
                vertices.put(keptByte(vertex, offset));
            }
            for (int offset = SectionMesh.VERTEX_BYTES; offset < GAME_VERTEX_BYTES; offset++) {
                vertices.put(NORMAL_BYTE);
            }
        }
        vertices.flip();
        return vertices;
    }

    static byte keptByte(int vertex, int offset) {
        return (byte) (vertex * 31 + offset);
    }

    static SectionGeometry solidQuads(int quadCount) {
        return new SectionMeshAssembler()
                .add(SectionLayer.SOLID, LayerQuads.inBuildOrder(gameQuads(quadCount), GAME_VERTEX_BYTES))
                .assemble();
    }
}
