package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.BlockLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class LevelStagesMixin {
    @Unique
    private static final int NO_DRAWN_SECTIONS = 0;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void minemod$leaveTheTerrainToTheScene(
            BlockLayer layer, Camera camera, CallbackInfoReturnable<Integer> callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        if (layer == BlockLayer.SOLID) {
            GameFrame.seeTerrain(camera.getPosition());
            GameFrame.enter(BatchSlot.SOLID);
        } else if (layer == BlockLayer.TRANSLUCENT) {
            GameFrame.enter(BatchSlot.BLENDED);
        }
        callback.setReturnValue(NO_DRAWN_SECTIONS);
    }
}
