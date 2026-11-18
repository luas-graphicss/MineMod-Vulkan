package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.TexelUploads;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import java.nio.IntBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class TextureCallsMixin {
    @Inject(method = "_genTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameATexture(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().textures().generate());
        }
    }

    @Inject(method = "_deleteTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$deleteATexture(int texture, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().delete(texture);
            callback.cancel();
        }
    }

    @Inject(method = "_bindTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$bindATexture(int texture, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().bind(texture);
            callback.cancel();
        }
    }

    @Inject(method = {"_activeTexture", "glActiveTexture"}, at = @At("HEAD"), cancellable = true)
    private static void minemod$activateAUnit(int unit, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().activate(unit);
            callback.cancel();
        }
    }

    @Inject(method = "_texParameter(III)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepASamplingParameter(int target, int parameter, int value, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().setParameter(target, parameter, value);
            callback.cancel();
        }
    }

    @Inject(method = "_texParameter(IIF)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$leaveLevelBiasesOut(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }

    @Inject(method = "_getTexLevelParameter", at = @At("HEAD"), cancellable = true)
    private static void minemod$tellALevelParameter(
            int target, int level, int parameter, CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().textures().levelParameter(target, level, parameter));
        }
    }

    @Inject(method = "_texImage2D", at = @At("HEAD"), cancellable = true)
    private static void minemod$specifyALevel(
            int target,
            int level,
            int internalFormat,
            int width,
            int height,
            int border,
            int format,
            int type,
            IntBuffer texels,
            CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().specify(target, level, internalFormat, width, height);
            callback.cancel();
        }
    }

    @Inject(method = "_texSubImage2D", at = @At("HEAD"), cancellable = true)
    private static void minemod$writeTexels(
            int target,
            int level,
            int x,
            int y,
            int width,
            int height,
            int format,
            int type,
            long texels,
            CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        if (TexelArea.isValid(level, x, y, width, height)) {
            TexelUploads.write(target, new TexelArea(level, x, y, width, height), format, type, texels);
        }
        callback.cancel();
    }

    @Inject(method = "_pixelStore", at = @At("HEAD"), cancellable = true)
    private static void minemod$keepAnUnpackingParameter(int parameter, int value, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().textures().store(parameter, value);
            callback.cancel();
        }
    }

    @Inject(method = "_getTexImage", at = @At("HEAD"), cancellable = true)
    private static void minemod$copyNoTexels(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
