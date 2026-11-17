package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.frame;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void minemod$drawTheHandOverTheWorld(
            PoseStack pose, Camera camera, float partialTick, CallbackInfo callback) {
        GameFrame.enter(BatchSlot.HAND);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void minemod$drawTheInterfaceOverTheWorld(
            float partialTick, long finishNanos, PoseStack pose, CallbackInfo callback) {
        GameFrame.enter(BatchSlot.INTERFACE);
    }
}
