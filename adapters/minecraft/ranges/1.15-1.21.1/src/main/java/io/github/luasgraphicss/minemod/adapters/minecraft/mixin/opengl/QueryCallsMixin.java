package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.OpenGlAnswers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class QueryCallsMixin {
    @Inject(method = "_getError", at = @At("HEAD"), cancellable = true)
    private static void minemod$reportNoError(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.NO_ERROR);
        }
    }

    @Inject(method = "_getString", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameTheRenderer(int name, CallbackInfoReturnable<String> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.string(name));
        }
    }

    @Inject(method = "_getInteger", at = @At("HEAD"), cancellable = true)
    private static void minemod$tellALimit(int name, CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.integer(name));
        }
    }
}
