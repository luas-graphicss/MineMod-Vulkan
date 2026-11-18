package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask$CompileResults")
public interface CompileResultsAccessor {
    @Accessor("renderedLayers")
    Map<RenderType, BufferBuilder.RenderedBuffer> minemod$renderedLayers();

    @Accessor("visibilitySet")
    VisibilitySet minemod$visibilitySet();
}
