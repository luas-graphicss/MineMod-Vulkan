package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class RetainedMesh {
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final ByteBuffer vertices;
    private final int vertexCount;
    private final boolean isSorted;

    private RetainedMesh(MeshData.DrawState uploaded, ByteBuffer vertices, boolean isSorted) {
        this.format = uploaded.format();
        this.mode = uploaded.mode();
        this.vertices = vertices;
        this.vertexCount = uploaded.vertexCount();
        this.isSorted = isSorted;
    }

    public static Optional<RetainedMesh> copyOf(MeshData mesh) {
        MeshData.DrawState uploaded = mesh.drawState();
        if (isPublishedAsASection(uploaded.format())) {
            return Optional.empty();
        }
        ByteBuffer source = mesh.vertexBuffer();
        ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining()).order(ByteOrder.nativeOrder());
        copy.put(source.duplicate());
        copy.flip();
        return Optional.of(new RetainedMesh(uploaded, copy, mesh.indexBuffer() != null));
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
