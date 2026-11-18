package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.terrain.SectionPosition;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
abstract class RenderSectionMixin {
    @Shadow
    @Final
    BlockPos.MutableBlockPos origin;

    @Inject(method = "reset", at = @At("HEAD"))
    private void minemod$unloadThePublishedGeometry(CallbackInfo callback) {
        SectionPosition position = SectionPosition.containingBlock(origin.getX(), origin.getY(), origin.getZ());
        ClientRendering.running().ifPresent(renderer -> renderer.terrain().sectionReset(position));
    }
}
