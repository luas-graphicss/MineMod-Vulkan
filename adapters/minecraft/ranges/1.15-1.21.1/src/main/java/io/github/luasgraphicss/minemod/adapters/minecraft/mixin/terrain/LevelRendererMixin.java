package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void minemod$leaveTheWorldBehind(ClientLevel level, CallbackInfo callback) {
        ClientRendering.running().ifPresent(renderer -> renderer.terrain().worldChanged());
    }
}
