package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.fixedfunction;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import java.nio.FloatBuffer;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class ShadingCallsMixin {
    @Inject(method = "_enableAlphaTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$cutFaintTexels(CallbackInfo callback) {
        minemod$change(callback, pipeline -> pipeline.alphaTest().enable(true));
    }

    @Inject(method = "_disableAlphaTest", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepFaintTexels(CallbackInfo callback) {
        minemod$change(callback, pipeline -> pipeline.alphaTest().enable(false));
    }

    @Inject(method = "_alphaFunc", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepTheAlphaComparison(int function, float reference, CallbackInfo callback) {
        minemod$change(callback, pipeline -> pipeline.alphaTest().compare(function, reference));
    }

    @Inject(method = "_enableLighting", at = @At("HEAD"), cancellable = true)
    private static void minemod$light(CallbackInfo callback) {
        minemod$change(callback, pipeline -> pipeline.lights().enable(true));
    }

    @Inject(method = "_disableLighting", at = @At("HEAD"), cancellable = true)
    private static void minemod$stopLighting(CallbackInfo callback) {
        minemod$change(callback, pipeline -> pipeline.lights().enable(false));
    }

    @Inject(method = "_light", at = @At("HEAD"), cancellable = true)
    private static void minemod$aimALight(int light, int name, FloatBuffer values, CallbackInfo callback) {
        minemod$change(callback, pipeline -> pipeline.lights()
                .parameter(light, name, values, pipeline.matrices().modelView().current()));
    }

    @Inject(
            method = {
                "_enableLight",
                "_lightModel",
                "_enableColorMaterial",
                "_disableColorMaterial",
                "_colorMaterial",
                "_normal3f",
                "_shadeModel",
                "_enableRescaleNormal",
                "_disableRescaleNormal"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveTheMaterialToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Unique
    private static void minemod$change(CallbackInfo callback, Consumer<FixedPipeline> change) {
        if (ClientRendering.replacesOpenGl()) {
            change.accept(FixedPipeline.current());
            callback.cancel();
        }
    }
}
