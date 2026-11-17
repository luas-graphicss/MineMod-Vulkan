package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.frame;

import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void minemod$drawTheHandOverTheWorld(
            Camera camera, float partialTick, Matrix4f projection, CallbackInfo callback) {
        GameFrame.enter(BatchSlot.HAND);
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void minemod$blurTheFrameInTheScene(float partialTick, CallbackInfo callback) {
        if (GameFrame.isRecording()) {
            GameFrame.blur(Minecraft.getInstance().options.getMenuBackgroundBlurriness());
            callback.cancel();
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void minemod$drawTheInterfaceOverTheWorld(DeltaTracker delta, CallbackInfo callback) {
        GameFrame.enter(BatchSlot.INTERFACE);
    }
}
