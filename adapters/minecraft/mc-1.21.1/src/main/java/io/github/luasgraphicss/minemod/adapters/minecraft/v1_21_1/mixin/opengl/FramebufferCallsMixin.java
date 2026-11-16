package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.opengl.OpenGlAnswers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class FramebufferCallsMixin {
    @Inject(method = {"glGenFramebuffers", "glGenRenderbuffers"}, at = @At("HEAD"), cancellable = true)
    private static void minemod$nameAFramebuffer(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().generateFramebuffer());
        }
    }

    @Inject(method = "_glBindFramebuffer", at = @At("HEAD"), cancellable = true)
    private static void minemod$drawIntoAFramebuffer(int target, int framebuffer, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().bindFramebuffer(target, framebuffer);
            callback.cancel();
        }
    }

    @Inject(method = "glCheckFramebufferStatus", at = @At("HEAD"), cancellable = true)
    private static void minemod$reportACompleteFramebuffer(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(OpenGlAnswers.FRAMEBUFFER_COMPLETE);
        }
    }

    @Inject(
            method = {
                "_glBlitFrameBuffer",
                "_glBindRenderbuffer",
                "_glDeleteRenderbuffers",
                "_glDeleteFramebuffers",
                "_glRenderbufferStorage",
                "_glFramebufferRenderbuffer",
                "_glFramebufferTexture2D",
                "_readPixels*",
                "_glDrawPixels"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveFramebuffersToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
