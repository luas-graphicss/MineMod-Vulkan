package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.terrain.FaceCapture;
import io.github.luasgraphicss.minemod.adapters.minecraft.terrain.SectionCapture;
import io.github.luasgraphicss.minemod.client.terrain.SectionPosition;
import io.github.luasgraphicss.minemod.client.terrain.TerrainPublisher;
import net.minecraft.client.renderer.chunk.ChunkCompileTask;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunk.class)
abstract class RenderChunkMixin {
    @Shadow
    @Final
    private BlockPos.MutableBlockPos origin;

    @Inject(
            method = "compile",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibilitySet(Lnet/minecraft/client/renderer/chunk/VisibilitySet;)V",
                            shift = At.Shift.AFTER))
    private void minemod$captureTheCompiledSection(
            float cameraX, float cameraY, float cameraZ, ChunkCompileTask task, CallbackInfo callback) {
        CompiledChunk compiled = task.getCompiledChunk();
        SectionPosition position = minemod$position();
        ClientRendering.running().ifPresent(renderer -> {
            TerrainPublisher terrain = renderer.terrain();
            terrain.describe(
                    position,
                    task::wasCancelled,
                    FaceCapture.connectionsOf(((CompiledChunkAccessor) compiled).minemod$visibilitySet()));
            terrain.capture(position, task::wasCancelled, SectionCapture.geometryOf(compiled, task.getBuilders()));
        });
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void minemod$unloadThePublishedGeometry(CallbackInfo callback) {
        SectionPosition position = minemod$position();
        ClientRendering.running().ifPresent(renderer -> renderer.terrain().sectionReset(position));
    }

    @Unique
    private SectionPosition minemod$position() {
        return SectionPosition.containingBlock(origin.getX(), origin.getY(), origin.getZ());
    }
}
