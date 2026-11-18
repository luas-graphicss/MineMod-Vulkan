package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.TextureUtil;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.opengl.PackedColorTexels;
import java.nio.IntBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureUtil.class)
abstract class TextureUtilMixin {
    @Inject(method = "initTexture", at = @At("HEAD"), cancellable = true)
    private static void minemod$writePackedColors(IntBuffer colors, int width, int height, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            PackedColorTexels.write(colors, width, height);
            callback.cancel();
        }
    }
}
