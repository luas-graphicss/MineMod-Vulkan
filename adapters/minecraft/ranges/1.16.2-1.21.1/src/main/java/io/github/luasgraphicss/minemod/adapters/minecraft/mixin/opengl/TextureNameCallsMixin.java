package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
abstract class TextureNameCallsMixin {
    @Inject(method = "_genTextures", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameTextures(int[] names, CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        TextureObjects textures = ClientRendering.openGl().textures();
        for (int index = 0; index < names.length; index++) {
            names[index] = textures.generate();
        }
        callback.cancel();
    }

    @Inject(method = "_deleteTextures", at = @At("HEAD"), cancellable = true)
    private static void minemod$deleteTextures(int[] deleted, CallbackInfo callback) {
        if (!ClientRendering.replacesOpenGl()) {
            return;
        }
        TextureObjects textures = ClientRendering.openGl().textures();
        for (int texture : deleted) {
            textures.delete(texture);
        }
        callback.cancel();
    }
}
