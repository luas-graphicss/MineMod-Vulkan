package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.TexelUploads;
import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class UnpaddedTexelsMixin {
    @Inject(method = "_upload", at = @At("HEAD"), cancellable = true)
    private static void minemod$writeUnpaddedTexels(
            int level,
            int x,
            int y,
            int width,
            int height,
            NativeImage.Format format,
            IntBuffer texels,
            Consumer<IntBuffer> release,
            CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        try {
            if (TexelArea.isValid(level, x, y, width, height)) {
                format.setUnpackPixelStoreState();
                TexelUploads.writeUnpadded(
                        new TexelArea(level, x, y, width, height), format.glFormat(), MemoryUtil.memAddress(texels));
            }
        } finally {
            release.accept(texels);
        }
        callback.cancel();
    }
}
