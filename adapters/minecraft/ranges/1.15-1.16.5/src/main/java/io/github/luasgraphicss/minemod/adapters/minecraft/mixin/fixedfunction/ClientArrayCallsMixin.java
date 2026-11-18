package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class ClientArrayCallsMixin {
    @Inject(method = "_drawArrays", at = @At("HEAD"), cancellable = true)
    private static void minemod$leaveOutADrawWithoutItsMesh(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            GameFrame.leaveOut();
            callback.cancel();
        }
    }

    @Inject(
            method = {
                "_vertexPointer",
                "_colorPointer",
                "_texCoordPointer",
                "_normalPointer",
                "_enableClientState",
                "_disableClientState"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveArraysToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
