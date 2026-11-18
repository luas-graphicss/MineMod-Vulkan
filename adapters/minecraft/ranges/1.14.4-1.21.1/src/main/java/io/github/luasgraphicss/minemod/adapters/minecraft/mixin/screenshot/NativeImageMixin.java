package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.screenshot;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.screenshot.FrameDownload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NativeImage.class)
abstract class NativeImageMixin {
    @Shadow
    @Final
    private NativeImage.Format format;

    @Shadow
    @Final
    private int width;

    @Shadow
    @Final
    private int height;

    @Shadow
    private long pixels;

    @Inject(method = "downloadTexture", at = @At("HEAD"), cancellable = true)
    private void minemod$downloadTheLastFrame(int level, boolean isOpaque, CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        if (format == NativeImage.Format.RGBA && level == 0) {
            FrameDownload.intoRgba(pixels, width, height, isOpaque);
        }
        callback.cancel();
    }
}
