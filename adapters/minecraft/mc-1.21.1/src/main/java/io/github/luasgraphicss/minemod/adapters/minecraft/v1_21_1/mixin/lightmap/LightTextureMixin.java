package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.mixin.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.lightmap.LightmapTexels;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
abstract class LightTextureMixin {
    @Shadow
    @Final
    private NativeImage lightPixels;

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void minemod$publishTheInitialLightmap(CallbackInfo callback) {
        minemod$publishTheLightmap();
    }

    @Inject(
            method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void minemod$publishTheUpdatedLightmap(float partialTicks, CallbackInfo callback) {
        minemod$publishTheLightmap();
    }

    @Unique
    private void minemod$publishTheLightmap() {
        ClientRendering.running().ifPresent(renderer -> renderer.lightmap().update(LightmapTexels.of(lightPixels)));
    }
}
