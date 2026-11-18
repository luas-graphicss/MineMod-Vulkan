package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompiledChunk.class)
public interface CompiledChunkAccessor {
    @Accessor("visibilitySet")
    VisibilitySet minemod$visibilitySet();
}
