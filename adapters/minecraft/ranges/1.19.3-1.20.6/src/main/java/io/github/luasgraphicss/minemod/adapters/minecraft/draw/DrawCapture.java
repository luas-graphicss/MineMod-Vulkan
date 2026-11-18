package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.adapters.minecraft.shaders.ReplacedShaders;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class DrawCapture {
    private static final int MATRIX_VALUES = 16;
    private static final int SAMPLED_UNIT = 0;

    private DrawCapture() {}

    public static void immediate(
            ByteBuffer contents, VertexFormat.Mode mode, VertexFormat format, int vertexCount, boolean isSorted) {
        if (vertexCount == 0) {
            return;
        }
        ByteBuffer vertices = contents.duplicate().order(ByteOrder.nativeOrder());
        vertices.clear();
        vertices.limit(vertexCount * format.getVertexSize());
        record(
                new Geometry(format, mode, vertices, vertexCount),
                isSorted,
                RenderSystem.getModelViewMatrix(),
                RenderSystem.getProjectionMatrix(),
                RenderSystem.getShader());
    }

    public static void leaveOut(BufferBuilder.DrawState drawn) {
        if (drawn.vertexCount() > 0) {
            GameFrame.leaveOut();
        }
    }

    public static void record(
            Geometry geometry, boolean isSorted, Matrix4f modelView, Matrix4f projection, ShaderInstance shader) {
        if (shader == null) {
            GameFrame.leaveOut();
            return;
        }
        Optional<PrimitiveMode> primitives = PrimitiveModes.of(geometry.mode());
        Optional<GameVertexLayout> layout = VertexLayouts.of(geometry.format());
        boolean isSampled = VertexLayouts.stores(geometry.format(), DefaultVertexFormat.ELEMENT_UV0)
                && ShaderTraits.samplesATexture(shader);
        Optional<ResourceHandle> texture =
                isSampled ? GameFrame.textures().find(RenderSystem.getShaderTexture(SAMPLED_UNIT)) : Optional.empty();
        if (GameFrame.leavesTheWorldOut()
                || !primitives.isPresent()
                || !layout.isPresent()
                || isSampled != texture.isPresent()
                || !GameFrame.drawsIntoTheWindow()) {
            GameFrame.leaveOut();
            return;
        }
        GameDraw.Builder draw = GameDraw.builder(
                        layout.get(), primitives.get(), geometry.vertices(), geometry.vertexCount())
                .view(SceneViews.seenThrough(projection, shader, geometry.format()))
                .state(GameFrame.state().renderState())
                .shading(ShaderTraits.shadingOf(shader))
                .program(ReplacedShaders.programOf(shader))
                .alphaCutoff(ShaderTraits.alphaCutoffOf(shader))
                .modelView(modelView.get(new float[MATRIX_VALUES]))
                .lineWidth(RenderSystem.getShaderLineWidth());
        texture.ifPresent(draw::texture);
        tint(draw, shader);
        moveTexture(draw, shader);
        if (isSorted) {
            draw.quadOrder(QuadOrders.current());
        }
        GameFrame.state().clipInto(draw);
        GameFrame.record(draw.build());
    }

    private static void tint(GameDraw.Builder draw, ShaderInstance shader) {
        if (shader.COLOR_MODULATOR != null) {
            float[] color = RenderSystem.getShaderColor();
            float glint = GlintAlpha.of(shader);
            draw.colorModulator(color[0] * glint, color[1] * glint, color[2] * glint, color[3]);
        }
    }

    private static void moveTexture(GameDraw.Builder draw, ShaderInstance shader) {
        if (shader.TEXTURE_MATRIX != null) {
            Matrix4f moved = RenderSystem.getTextureMatrix();
            draw.textureTransform(
                    new float[] {moved.m00(), moved.m10(), moved.m30()},
                    new float[] {moved.m01(), moved.m11(), moved.m31()});
        }
    }

    public static final class Geometry {
        private final VertexFormat format;
        private final VertexFormat.Mode mode;
        private final ByteBuffer vertices;
        private final int vertexCount;

        public Geometry(VertexFormat format, VertexFormat.Mode mode, ByteBuffer vertices, int vertexCount) {
            this.format = format;
            this.mode = mode;
            this.vertices = vertices;
            this.vertexCount = vertexCount;
        }

        VertexFormat format() {
            return format;
        }

        VertexFormat.Mode mode() {
            return mode;
        }

        ByteBuffer vertices() {
            return vertices;
        }

        int vertexCount() {
            return vertexCount;
        }
    }
}
