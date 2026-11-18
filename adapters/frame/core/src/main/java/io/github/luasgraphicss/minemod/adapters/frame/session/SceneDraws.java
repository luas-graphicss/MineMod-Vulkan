package io.github.luasgraphicss.minemod.adapters.frame.session;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.adapters.frame.draw.CapturedDraw;
import io.github.luasgraphicss.minemod.client.draw.frame.ClipSpace;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.draw.frame.GameView;
import io.github.luasgraphicss.minemod.client.draw.texture.SceneTextures;
import io.github.luasgraphicss.minemod.codec.scene.FogShape;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

final class SceneDraws {
    private SceneDraws() {}

    static Optional<GameDraw> of(CapturedDraw draw, SceneTextures textures) {
        Set<Shading> shading = EnumSet.noneOf(Shading.class);
        shading.addAll(draw.shading());
        Optional<ResourceHandle> sampled =
                shading.contains(Shading.TEXTURE) ? textures.find(draw.sampledTexture()) : Optional.empty();
        if (shading.contains(Shading.TEXTURE) && !sampled.isPresent()) {
            return Optional.empty();
        }
        GameView.Builder view = GameView.builder(ClipSpace.fromOpenGl(draw.projection()));
        draw.fogColor().ifPresent(color -> view.fog(color, draw.fogStart(), draw.fogEnd(), FogShape.SPHERE));
        draw.lights().ifPresent(lights -> view.lights(lights[0], lights[1]));
        if (shading.contains(Shading.LIGHTMAP)) {
            Optional<ResourceHandle> lightmap = textures.find(draw.lightmapTexture());
            if (lightmap.isPresent()) {
                view.lightmap(lightmap.get());
            } else {
                shading.remove(Shading.LIGHTMAP);
            }
        }
        Geometry geometry = draw.geometry();
        GameDraw.Builder scene = GameDraw.builder(
                        geometry.layout(), draw.mode(), geometry.vertices(), geometry.vertexCount())
                .view(view.build())
                .state(draw.state())
                .shading(shading)
                .alphaCutoff(draw.alphaCutoff())
                .modelView(draw.modelView())
                .lineWidth(draw.lineWidth());
        sampled.ifPresent(scene::texture);
        draw.textureRows().ifPresent(rows -> scene.textureTransform(rows[0], rows[1]));
        draw.color().ifPresent(color -> scene.colorModulator(color[0], color[1], color[2], color[3]));
        return Optional.of(scene.build());
    }
}
