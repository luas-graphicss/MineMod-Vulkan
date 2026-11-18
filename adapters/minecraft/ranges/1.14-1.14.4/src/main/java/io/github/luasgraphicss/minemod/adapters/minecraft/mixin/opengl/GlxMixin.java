package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GLX.class)
abstract class GlxMixin {
    @Redirect(
            method = "init",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lorg/lwjgl/opengl/GL;getCapabilities()Lorg/lwjgl/opengl/GLCapabilities;",
                            remap = false))
    private static GLCapabilities minemod$askNoDriverForItsCapabilities() {
        return null;
    }

    @Redirect(
            method = "init",
            at = @At(value = "FIELD", target = "Lorg/lwjgl/opengl/GLCapabilities;*:Z", remap = false))
    private static boolean minemod$supportEveryCapability(GLCapabilities capabilities) {
        return true;
    }

    @Redirect(
            method = "init",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetString(I)Ljava/lang/String;", remap = false))
    private static String minemod$nameTheRenderer(int name) {
        return GlStateManager.getString(name);
    }

    @Inject(method = "setupNvFogDistance", at = @At("HEAD"), cancellable = true)
    private static void minemod$measureFogFromTheCamera(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Inject(method = "supportsOpenGL2", at = @At("HEAD"), cancellable = true)
    private static void minemod$supportShaders(CallbackInfoReturnable<Boolean> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(true);
        }
    }

    @Redirect(
            method = "withTextureRestore",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPushAttrib(I)V", remap = false))
    private static void minemod$saveTheSampledUnits(int attributes) {
        GlStateManager.pushTextureAttributes();
    }

    @Redirect(
            method = "withTextureRestore",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopAttrib()V", remap = false))
    private static void minemod$restoreTheSampledUnits() {
        GlStateManager.popAttributes();
    }

    @Redirect(
            method = "renderCrosshair(IZZZ)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLineWidth(F)V", remap = false),
            require = 3)
    private static void minemod$widenTheCrosshair(float pixels) {
        GlStateManager.lineWidth(pixels);
    }
}
