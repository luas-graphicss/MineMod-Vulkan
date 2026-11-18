package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FogSettings;
import java.nio.FloatBuffer;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class FogCallsMixin {
    @Inject(method = "enableFog", at = @At("HEAD"), cancellable = true)
    private static void minemod$fog(CallbackInfo callback) {
        minemod$change(callback, fog -> fog.enable(true));
    }

    @Inject(method = "disableFog", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopFogging(CallbackInfo callback) {
        minemod$change(callback, fog -> fog.enable(false));
    }

    @Inject(method = "fogMode(I)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogEquation(int mode, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.mode(mode));
    }

    @Inject(method = "fogDensity", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogDensity(float density, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.density(density));
    }

    @Inject(method = "fogStart", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogStart(float distance, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.start(distance));
    }

    @Inject(method = "fogEnd", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogEnd(float distance, CallbackInfo callback) {
        minemod$change(callback, fog -> fog.end(distance));
    }

    @Inject(method = "fog", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheFogColor(int name, FloatBuffer values, CallbackInfo callback) {
        float[] parameter = new float[values.remaining()];
        values.duplicate().get(parameter);
        minemod$change(callback, fog -> fog.parameter(name, parameter));
    }

    @Inject(method = "fogi", at = @At("HEAD"), cancellable = true)
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
