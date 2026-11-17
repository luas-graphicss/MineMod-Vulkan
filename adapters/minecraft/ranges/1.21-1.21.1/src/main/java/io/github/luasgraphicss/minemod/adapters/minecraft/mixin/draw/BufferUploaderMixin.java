package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.DrawCapture;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferUploader.class)
abstract class BufferUploaderMixin {
    @Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
    private static void minemod$recordTheDraw(MeshData mesh, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        try (MeshData drawn = mesh) {
            DrawCapture.immediate(drawn);
        }
        callback.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void minemod$leaveOutADrawWithoutItsShader(MeshData mesh, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        try (MeshData drawn = mesh) {
            DrawCapture.leaveOut(drawn);
        }
        callback.cancel();
    }
}
