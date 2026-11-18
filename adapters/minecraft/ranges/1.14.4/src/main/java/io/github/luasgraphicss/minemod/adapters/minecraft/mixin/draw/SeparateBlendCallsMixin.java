package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.platform.GLX;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.compatibility.ModRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.client.compatibility.state.WrapperCall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLX.class)
abstract class SeparateBlendCallsMixin {
    @Inject(method = "glBlendFuncSeparate", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheSeparateBlendFactors(
            int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha, CallbackInfo callback) {
        ModRendering.left(WrapperCall.BLEND_FACTORS, sourceColor, destinationColor, sourceAlpha, destinationAlpha);
        if (ClientRendering.replacesOpenGl()) {
            GameFrame.state().blendFactors(sourceColor, destinationColor);
            callback.cancel();
        }
    }
}
