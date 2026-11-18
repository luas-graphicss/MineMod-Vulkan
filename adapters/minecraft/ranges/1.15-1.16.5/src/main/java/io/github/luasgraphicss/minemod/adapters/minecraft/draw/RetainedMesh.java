package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public final class RetainedMesh {
    private final VertexFormat format;
    private final ByteBuffer vertices;
    private final int vertexCount;

    private RetainedMesh(VertexFormat format, ByteBuffer vertices, int vertexCount) {
        this.format = format;
        this.vertices = vertices;
        this.vertexCount = vertexCount;
    }

    public static Optional<RetainedMesh> copyOf(BufferBuilder.DrawState uploaded, ByteBuffer contents) {
        if (isPublishedAsASection(uploaded.format())) {
            return Optional.empty();
        }
        ByteBuffer source = contents.duplicate();
        ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining()).order(ByteOrder.nativeOrder());
        copy.put(source);
        copy.flip();
        return Optional.of(new RetainedMesh(uploaded.format(), copy, uploaded.vertexCount()));
    }

    public void record(Matrix4f modelView, int mode) {
        DrawCapture.record(
                new DrawCapture.Geometry(format, mode, vertices.duplicate(), vertexCount),
                Matrices.columnMajor(modelView));
    }

    private static boolean isPublishedAsASection(VertexFormat format) {
        return format.equals(DefaultVertexFormat.BLOCK);
    }
}
