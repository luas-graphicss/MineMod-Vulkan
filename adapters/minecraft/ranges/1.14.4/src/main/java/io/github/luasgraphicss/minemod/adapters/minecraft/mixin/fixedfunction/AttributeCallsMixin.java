package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class AttributeCallsMixin {
    @Inject(method = {"pushLightingAttributes", "pushTextureAttributes"}, at = @At("HEAD"), cancellable = true)
    private static void minemod$saveTheEnabledSteps(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            FixedPipeline.current().saveSteps();
            callback.cancel();
        }
    }

    @Inject(method = "popAttributes", at = @At("HEAD"), cancellable = true)
    private static void minemod$restoreTheEnabledSteps(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            FixedPipeline.current().restoreSteps();
            callback.cancel();
        }
    }
}
