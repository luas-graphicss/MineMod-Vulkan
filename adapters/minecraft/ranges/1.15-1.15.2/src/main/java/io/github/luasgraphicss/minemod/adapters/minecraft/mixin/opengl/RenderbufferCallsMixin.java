package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class RenderbufferCallsMixin {
    @Inject(method = "glGenRenderbuffers", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameARenderbuffer(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().generateFramebuffer());
        }
    }

    @Inject(
            method = {
                "_glBindRenderbuffer",
                "_glDeleteRenderbuffers",
                "_glRenderbufferStorage",
                "_glFramebufferRenderbuffer"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveRenderbuffersToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
