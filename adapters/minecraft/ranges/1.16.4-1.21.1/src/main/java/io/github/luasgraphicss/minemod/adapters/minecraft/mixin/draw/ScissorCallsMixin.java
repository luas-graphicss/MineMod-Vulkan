package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.client.compatibility.state.WrapperCall;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class ScissorCallsMixin {
    @Inject(method = "_enableScissorTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$clip(CallbackInfo callback) {
        minemod$keepClipping(callback, true);
    }

    @Inject(method = "_disableScissorTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopClipping(CallbackInfo callback) {
        minemod$keepClipping(callback, false);
    }

    @Inject(method = "_scissorBox", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheClipBox(int left, int bottom, int width, int height, CallbackInfo callback) {
        ModRendering.left(WrapperCall.SCISSOR_BOX, left, bottom, width, height);
        if (ClientRendering.replacesOpenGl()) {
            GameFrame.state().clipBox(left, bottom, width, height);
            callback.cancel();
        }
    }

    @Unique
    private static void minemod$keepClipping(CallbackInfo callback, boolean isEnabled) {
        if (ClientRendering.replacesOpenGl()) {
            ModRendering.capability(GL11.GL_SCISSOR_TEST, isEnabled);
            GameFrame.state().clip(isEnabled);
            callback.cancel();
        }
    }
}
