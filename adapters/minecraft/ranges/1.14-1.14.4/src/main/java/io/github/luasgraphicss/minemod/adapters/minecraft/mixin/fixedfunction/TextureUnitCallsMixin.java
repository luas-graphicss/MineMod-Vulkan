package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.TextureUnits;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class TextureUnitCallsMixin {
    @Inject(method = "enableTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$sample(CallbackInfo callback) {
        minemod$change(callback, units -> units.enable(true));
    }

    @Inject(method = "disableTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopSampling(CallbackInfo callback) {
        minemod$change(callback, units -> units.enable(false));
    }

    @Inject(method = "enableTexGen", at = @At("HEAD"), cancellable = true)
    private static void minemod$generateCoordinates(CallbackInfo callback) {
        minemod$change(callback, units -> units.generateCoordinates(true));
    }

    @Inject(method = "disableTexGen", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopGeneratingCoordinates(CallbackInfo callback) {
        minemod$change(callback, units -> units.generateCoordinates(false));
    }

    @Inject(
            method = {"texEnv*", "texGenMode", "texGenParam"},
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveTheCombinationToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Unique
    private static void minemod$change(CallbackInfo callback, Consumer<TextureUnits> change) {
        if (ClientRendering.replacesOpenGl()) {
            change.accept(FixedPipeline.current().units());
            callback.cancel();
        }
    }
}
