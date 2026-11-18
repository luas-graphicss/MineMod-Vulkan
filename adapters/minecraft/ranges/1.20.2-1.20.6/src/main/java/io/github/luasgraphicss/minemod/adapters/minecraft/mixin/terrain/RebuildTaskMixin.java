package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.terrain.FaceCapture;
import io.github.luasgraphicss.minemod.adapters.minecraft.terrain.SectionCapture;
import io.github.luasgraphicss.minemod.client.terrain.SectionGeometry;
import io.github.luasgraphicss.minemod.client.terrain.SectionPosition;
import io.github.luasgraphicss.minemod.codec.scene.FaceConnections;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
abstract class RebuildTaskMixin {
    @Shadow
    @Final
    SectionRenderDispatcher.RenderSection field_20839;

    @Inject(method = "compile", at = @At("RETURN"))
    private void minemod$captureTheCompiledSection(
            float cameraX,
            float cameraY,
            float cameraZ,
            SectionBufferBuilderPack buffers,
            CallbackInfoReturnable<CompileResultsAccessor> callback) {
        CompileResultsAccessor compiled = callback.getReturnValue();
        minemod$describe(FaceCapture.connectionsOf(compiled.minemod$visibilitySet()));
        minemod$capture(SectionCapture.geometryOf(compiled.minemod$renderedLayers()));
    }

    @Unique
    private void minemod$capture(SectionGeometry geometry) {
        SectionPosition position = minemod$position();
        AtomicBoolean isCancelled = ((CompileTaskAccessor) this).minemod$isCancelled();
        ClientRendering.running().ifPresent(renderer -> renderer.terrain().capture(position, isCancelled::get, geometry));
    }

    @Unique
    private void minemod$describe(FaceConnections connections) {
        SectionPosition position = minemod$position();
        AtomicBoolean isCancelled = ((CompileTaskAccessor) this).minemod$isCancelled();
        ClientRendering.running()
                .ifPresent(renderer -> renderer.terrain().describe(position, isCancelled::get, connections));
    }

    @Unique
    private SectionPosition minemod$position() {
        BlockPos origin = field_20839.getOrigin();
        return SectionPosition.containingBlock(origin.getX(), origin.getY(), origin.getZ());
    }
}
