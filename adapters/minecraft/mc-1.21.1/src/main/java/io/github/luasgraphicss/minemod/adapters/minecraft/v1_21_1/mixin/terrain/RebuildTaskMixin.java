package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.terrain;

import com.mojang.blaze3d.vertex.MeshData;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.terrain.SectionCapture;
import io.github.luasgraphicss.minemod.client.terrain.SectionGeometry;
import io.github.luasgraphicss.minemod.client.terrain.SectionPosition;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
abstract class RebuildTaskMixin {
    @Shadow
    @Final
    SectionRenderDispatcher.RenderSection field_20839;

    @Redirect(
            method = "doTask",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V",
                            remap = false))
    private void minemod$captureTheCompiledLayers(
            Map<RenderType, MeshData> compiledLayers, BiConsumer<RenderType, MeshData> upload) {
        minemod$capture(SectionCapture.geometryOf(compiledLayers));
        compiledLayers.forEach(upload);
    }

    @Inject(
            method = "doTask",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setCompiled(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$CompiledSection;)V"))
    private void minemod$captureASectionWithoutBlocks(
            SectionBufferBuilderPack buffers, CallbackInfoReturnable<CompletableFuture<?>> callback) {
        minemod$capture(SectionGeometry.empty());
    }

    @Unique
    private void minemod$capture(SectionGeometry geometry) {
        BlockPos origin = field_20839.getOrigin();
        SectionPosition position = SectionPosition.containingBlock(origin.getX(), origin.getY(), origin.getZ());
        AtomicBoolean isCancelled = ((CompileTaskAccessor) this).minemod$isCancelled();
        ClientRendering.running().ifPresent(renderer -> renderer.terrain().capture(position, isCancelled::get, geometry));
    }
}
