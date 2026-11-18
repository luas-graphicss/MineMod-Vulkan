package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.math.Matrix4f;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.FogShapes;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.Matrices;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FogSettings;
import io.github.luasgraphicss.minemod.client.draw.frame.ClipSpace;
import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.client.lightmap.LightmapPublisher;
import io.github.luasgraphicss.minemod.codec.scene.Fog;
import io.github.luasgraphicss.minemod.codec.scene.TerrainState;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public final class TerrainView {
    private static final float FAR_FOG_START = 1.0e30f;
    private static final float FAR_FOG_END = 2.0e30f;

    private TerrainView() {}

    public static Optional<TerrainState> seenFrom(
            Vec3 camera, Matrix4f frustum, Matrix4f projection, SceneTextures textures, LightmapPublisher lightmap) {
        if (!lightmap.isDefined()) {
            return Optional.empty();
        }
        float[] viewProjection = ClipSpace.fromOpenGl(Matrices.product(projection, frustum));
        Fog fog = fog();
        return textures.find(blockAtlas())
                .map(atlas -> new TerrainState(
                        viewProjection, camera.x(), camera.y(), camera.z(), fog, atlas, lightmap.texture()));
    }

    private static int blockAtlas() {
        return Minecraft.getInstance()
                .getTextureManager()
                .getTexture(InventoryMenu.BLOCK_ATLAS)
                .getId();
    }

    private static Fog fog() {
        FogSettings settings = FixedPipeline.current().fog();
        float[] color = settings.color();
        float start = settings.start();
        float end = settings.end();
        boolean isMeasurable = end > start;
        return new Fog(
                color[0],
                color[1],
                color[2],
                color[3],
                isMeasurable ? start : FAR_FOG_START,
                isMeasurable ? end : FAR_FOG_END,
                FogShapes.current());
    }
}
