package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FogSettings;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class FogCallsMixin {
    @Inject(method = "_enableFog", at = @At("HEAD"), cancellable = true)
    private static void minemod$fog(CallbackInfo callback) {
        minemod$change(callback, fog -> fog.enable(true));
    }

    @Inject(method = "_disableFog", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopFogging(CallbackInfo callback) {
        minemod$change(callback, fog -> fog.enable(false));
    }

    @Inject(method = "_fogMode", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogEquation(int mode, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.mode(mode));
    }

    @Inject(method = "_fogDensity", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogDensity(float density, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.density(density));
    }

    @Inject(method = "_fogStart", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogStart(float distance, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.start(distance));
    }

    @Inject(method = "_fogEnd", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogEnd(float distance, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.end(distance));
    }

    @Inject(method = "_fog", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogColor(int name, float[] values, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.parameter(name, values));
    }

    @Inject(method = "_fogi", at = @At("HEAD"), cancellable = true)
    private static void minemod$measureFogFromTheCamera(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Unique
    private static void minemod$change(CallbackInfo callback, Consumer<FogSettings> change) {
        if (ClientRendering.replacesOpenGl()) {
            change.accept(FixedPipeline.current().fog());
            callback.cancel();
        }
    }
}
