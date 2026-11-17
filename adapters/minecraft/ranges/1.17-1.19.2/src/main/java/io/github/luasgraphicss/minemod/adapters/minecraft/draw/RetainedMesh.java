package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import net.minecraft.client.renderer.ShaderInstance;

public final class RetainedMesh {
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final ByteBuffer vertices;
    private final int vertexCount;
    private final boolean isSorted;

    private RetainedMesh(BufferBuilder.DrawState uploaded, ByteBuffer vertices) {
        this.format = uploaded.format();
        this.mode = uploaded.mode();
        this.vertices = vertices;
        this.vertexCount = uploaded.vertexCount();
        this.isSorted = !uploaded.sequentialIndex();
    }

    public static Optional<RetainedMesh> copyOf(BufferBuilder.DrawState uploaded, ByteBuffer contents) {
        if (uploaded.indexOnly() || isPublishedAsASection(uploaded.format())) {
            return Optional.empty();
        }
        ByteBuffer source = contents.duplicate();
        source.limit(source.position() + uploaded.vertexBufferSize());
        ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining()).order(ByteOrder.nativeOrder());
        copy.put(source);
        copy.flip();
        return Optional.of(new RetainedMesh(uploaded, copy));
    }

    public void record(Matrix4f modelView, Matrix4f projection, ShaderInstance shader) {
        DrawCapture.record(
                new DrawCapture.Geometry(format, mode, vertices.duplicate(), vertexCount),
                isSorted,
                modelView,
                projection,
                shader);
    }

    private static boolean isPublishedAsASection(VertexFormat format) {
        return format.equals(DefaultVertexFormat.BLOCK);
    }
}
