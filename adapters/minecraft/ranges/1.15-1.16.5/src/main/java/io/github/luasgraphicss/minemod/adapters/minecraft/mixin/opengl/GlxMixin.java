package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.compatibility.ModRendering;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GLX.class)
abstract class GlxMixin {
    @Redirect(
            method = "_init",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lorg/lwjgl/opengl/GL;getCapabilities()Lorg/lwjgl/opengl/GLCapabilities;",
                            remap = false))
    private static GLCapabilities minemod$startWithTheCapabilitiesOfMods() {
        return ModRendering.running().isPresent() ? GL.getCapabilities() : null;
    }

    @Inject(method = "_setupNvFogDistance", at = @At("HEAD"), cancellable = true)
    private static void minemod$measureFogFromTheCamera(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Redirect(
            method = "_renderCrosshair",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLineWidth(F)V", remap = false),
            require = 3)
    private static void minemod$widenTheCrosshair(float pixels) {
        GlStateManager._lineWidth(pixels);
    }
}
