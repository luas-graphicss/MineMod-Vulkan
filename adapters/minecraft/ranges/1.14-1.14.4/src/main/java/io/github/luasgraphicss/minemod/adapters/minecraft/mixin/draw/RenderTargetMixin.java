package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
abstract class RenderTargetMixin {
    @Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
    private void minemod$drawIntoThisTarget(boolean setsViewport, CallbackInfo callback) {
        if (GameFrame.isRecording()) {
            GameFrame.state().drawInto(this);
            callback.cancel();
        }
    }

    @Inject(method = "blitToScreen(IIZ)V", at = @At("HEAD"), cancellable = true)
    private void minemod$presentWithoutCopyingTheTarget(CallbackInfo callback) {
        if (GameFrame.isRecording()) {
            callback.cancel();
        }
    }

    @Inject(method = "unbindWrite", at = @At("HEAD"), cancellable = true)
    private void minemod$drawIntoNoTarget(CallbackInfo callback) {
        if (GameFrame.isRecording()) {
            GameFrame.state().drawInto(null);
            callback.cancel();
        }
    }
}
