package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class SurfaceCallsMixin {
    @Inject(method = "_color4f", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheColor(float red, float green, float blue, float alpha, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            FixedPipeline.current().color(red, green, blue, alpha);
            callback.cancel();
        }
    }

    @Inject(method = "_lineWidth", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheLineWidth(float pixels, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            FixedPipeline.current().lineWidth(pixels);
            callback.cancel();
        }
    }

    @Inject(
            method = {"_enableLineOffset", "_disableLineOffset", "_pixelTransfer", "_blendColor"},
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveTheRestToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
