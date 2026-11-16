package io.github.luasgraphicss.minemod.client.terrain;

import io.github.luasgraphicss.minemod.codec.scene.SectionMesh;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SectionMeshAssembler {
    private static final int RANGE_VALUES = 2;

    private final Map<SectionLayer, List<LayerQuads>> layers = new EnumMap<>(SectionLayer.class);

    public SectionMeshAssembler add(SectionLayer layer, LayerQuads quads) {
        layers.computeIfAbsent(layer, added -> new ArrayList<>()).add(quads);
        return this;
    }

    public SectionGeometry assemble() {
        int vertexCount = 0;
        int indexCount = 0;
        for (List<LayerQuads> batches : layers.values()) {
            for (LayerQuads quads : batches) {
                vertexCount += quads.vertexCount();
                indexCount += quads.indexCount();
            }
        }
        ByteBuffer vertices = ByteBuffer.allocate(vertexCount * SectionMesh.VERTEX_BYTES);
        ByteBuffer indices = ByteBuffer.allocate(indexCount * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        int[] layerRanges = new int[SectionMesh.LAYER_COUNT * RANGE_VALUES];
        for (SectionLayer layer : SectionLayer.values()) {
            int firstIndex = indices.position() / Integer.BYTES;
            for (LayerQuads quads : layers.getOrDefault(layer, Collections.<LayerQuads>emptyList())) {
                quads.appendIndices(indices, vertices.position() / SectionMesh.VERTEX_BYTES);
                quads.copyVertices(vertices);
            }
            layerRanges[layer.index() * RANGE_VALUES] = firstIndex;
            layerRanges[layer.index() * RANGE_VALUES + 1] = indices.position() / Integer.BYTES - firstIndex;
        }
        vertices.flip();
        indices.flip();
        return new SectionGeometry(vertices, indices, layerRanges);
    }
}
