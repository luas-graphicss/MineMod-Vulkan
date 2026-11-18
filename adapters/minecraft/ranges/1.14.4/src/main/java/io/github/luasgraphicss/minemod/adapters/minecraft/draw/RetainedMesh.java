package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public final class RetainedMesh {
    private static final ByteBuffer NO_VERTICES = ByteBuffer.allocate(0);

    private final VertexFormat format;
    private final ByteBuffer vertices;

    private RetainedMesh(VertexFormat format, ByteBuffer vertices) {
        this.format = format;
        this.vertices = vertices;
    }

    public static Optional<RetainedMesh> uploadedAs(VertexFormat format, ByteBuffer contents) {
        if (isPublishedAsASection(format)) {
            return Optional.empty();
        }
        return Optional.of(copyOf(format, contents));
    }

    static RetainedMesh copyOf(VertexFormat format, ByteBuffer contents) {
        return new RetainedMesh(format, joined(NO_VERTICES, contents));
    }

    RetainedMesh joinedWith(ByteBuffer contents) {
        return new RetainedMesh(format, joined(vertices, contents));
    }

    boolean isStoredAs(VertexFormat other) {
        return format.equals(other);
    }

    public void record(int mode) {
        DrawCapture.record(
                new DrawCapture.Geometry(
                        format, mode, vertices.duplicate(), vertices.remaining() / format.getVertexSize()),
                FixedPipeline.current().matrices().modelView().current());
    }

    private static ByteBuffer joined(ByteBuffer first, ByteBuffer second) {
        ByteBuffer joined =
                ByteBuffer.allocateDirect(first.remaining() + second.remaining()).order(ByteOrder.nativeOrder());
        joined.put(first.duplicate()).put(second.duplicate());
        joined.flip();
        return joined;
    }

    static boolean isPublishedAsASection(VertexFormat format) {
        return format.equals(DefaultVertexFormat.BLOCK);
    }
}
