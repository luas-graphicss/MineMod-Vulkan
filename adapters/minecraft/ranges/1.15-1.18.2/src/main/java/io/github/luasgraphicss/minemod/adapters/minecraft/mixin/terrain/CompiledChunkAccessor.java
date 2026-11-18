package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkRenderDispatcher.CompiledChunk.class)
public interface CompiledChunkAccessor {
    @Accessor("hasLayer")
    Set<RenderType> minemod$compiledLayers();

    @Accessor("visibilitySet")
    VisibilitySet minemod$visibilitySet();
}
