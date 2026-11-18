package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw.RenderSystemAccessor;
import io.github.luasgraphicss.minemod.client.draw.frame.ClipSpace;
import io.github.luasgraphicss.minemod.client.draw.frame.GameView;
import io.github.luasgraphicss.minemod.client.lightmap.LightmapPublisher;
import io.github.luasgraphicss.minemod.client.overlay.OverlayPublisher;
import io.github.luasgraphicss.minemod.codec.scene.FogShape;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SceneViews {
    private static final int MATRIX_VALUES = 16;

    private SceneViews() {}

    static GameView seenThrough(Matrix4f projection, ShaderInstance shader, VertexFormat format) {
        GameView.Builder view = GameView.builder(ClipSpace.fromOpenGl(projection.get(new float[MATRIX_VALUES])));
        if (shader.FOG_START != null) {
            view.fog(
                    RenderSystem.getShaderFogColor(),
                    RenderSystem.getShaderFogStart(),
                    RenderSystem.getShaderFogEnd(),
                    shapeOf(RenderSystem.getShaderFogShape()));
        }
        Vector3f[] lights = RenderSystemAccessor.minemod$lightDirections();
        if (shader.LIGHT0_DIRECTION != null && lights[0] != null && lights[1] != null) {
            view.lights(directionOf(lights[0]), directionOf(lights[1]));
        }
        LightmapPublisher lightmap = ClientRendering.renderer().lightmap();
        if (VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_UV2) && lightmap.isDefined()) {
            view.lightmap(lightmap.texture());
        }
        OverlayPublisher overlay = ClientRendering.renderer().overlay();
        if (VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_UV1) && overlay.isDefined()) {
            view.overlay(overlay.texture());
        }
        return view.build();
    }

    public static FogShape shapeOf(com.mojang.blaze3d.shaders.FogShape shape) {
        return shape == com.mojang.blaze3d.shaders.FogShape.CYLINDER ? FogShape.CYLINDER : FogShape.SPHERE;
    }

    private static float[] directionOf(Vector3f light) {
        return new float[] {light.x(), light.y(), light.z()};
    }
}
