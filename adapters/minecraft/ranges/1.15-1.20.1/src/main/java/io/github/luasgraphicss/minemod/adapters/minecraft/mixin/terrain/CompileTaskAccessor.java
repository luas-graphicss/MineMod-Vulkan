package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$ChunkCompileTask")
public interface CompileTaskAccessor {
    @Accessor("isCancelled")
    AtomicBoolean minemod$isCancelled();
}
