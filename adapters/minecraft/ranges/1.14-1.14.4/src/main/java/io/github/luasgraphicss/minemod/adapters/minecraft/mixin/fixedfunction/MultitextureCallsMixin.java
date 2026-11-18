package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GLX;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.TextureUnits;
import io.github.luasgraphicss.minemod.adapters.minecraft.lightmap.ConstantLight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLX.class)
abstract class MultitextureCallsMixin {
    @Inject(method = "glActiveTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$activateAUnit(int unit, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().activate(unit);
            callback.cancel();
        }
    }

    @Inject(method = "glMultiTexCoord2f", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheLightOfTheWholeDraw(int unit, float u, float v, CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        if (unit == TextureUnits.FIRST_NAME + TextureUnits.LIGHTMAP) {
            ConstantLight.coordinate(u, v);
        }
        callback.cancel();
    }

    @Inject(method = "glClientActiveTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$leaveArraysToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
