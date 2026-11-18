package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.overlay;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.overlay.OverlayTexels;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OverlayTexture.class)
abstract class OverlayTextureMixin {
    @Shadow
    @Final
    private DynamicTexture texture;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void minemod$publishTheOverlay(CallbackInfo callback) {
        ClientRendering.running()
                .ifPresent(renderer -> renderer.overlay().update(OverlayTexels.of(texture.getPixels())));
    }
}
