package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.frame;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.codec.scene.BatchSlot;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelStagesMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void minemod$startWithTheSky(
            PoseStack pose,
            float partialTick,
            long finishNanos,
            boolean outlinesBlock,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightmap,
            Matrix4f projection,
            CallbackInfo callback) {
        GameFrame.enterWorld(camera.getPosition(), pose.last().pose(), projection);
    }

    @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true)
    private void minemod$leaveTheTerrainToTheScene(
            RenderType layer,
            PoseStack pose,
            double cameraX,
            double cameraY,
            double cameraZ,
            Matrix4f projection,
            CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        if (layer == RenderType.solid()) {
            GameFrame.seeTerrain(new Vec3(cameraX, cameraY, cameraZ), pose.last().pose(), projection);
            GameFrame.enter(BatchSlot.SOLID);
        } else if (layer == RenderType.translucent()) {
            GameFrame.enter(BatchSlot.BLENDED);
        }
        callback.cancel();
    }
}
