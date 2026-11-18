package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.DisplayLists;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class DisplayListCallsMixin {
    @Inject(method = "genLists", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameLists(int count, CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(DisplayLists.current().generate(count));
        }
    }

    @Inject(method = "newList", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheNextDraws(int list, int mode, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            DisplayLists.current().begin(list);
            callback.cancel();
        }
    }

    @Inject(method = "endList", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopKeepingDraws(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            DisplayLists.current().end();
            callback.cancel();
        }
    }

    @Inject(method = "callList", at = @At("HEAD"), cancellable = true)
    private static void minemod$recordTheKeptDraws(int list, CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        if (GameFrame.isRecording()) {
            DisplayLists.current().call(list);
        }
        callback.cancel();
    }

    @Inject(method = "deleteLists", at = @At("HEAD"), cancellable = true)
    private static void minemod$forgetLists(int first, int count, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            DisplayLists.current().delete(first, count);
            callback.cancel();
        }
    }
}
