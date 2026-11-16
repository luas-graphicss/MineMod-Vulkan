package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.terrain;

import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public interface CompileTaskAccessor {
    @Accessor("isCancelled")
    AtomicBoolean minemod$isCancelled();
}
