package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.DrawCapture;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferUploader.class)
abstract class BufferUploaderMixin {
    @Inject(method = "_end", at = @At("HEAD"), cancellable = true)
    private static void minemod$recordTheDraw(
            ByteBuffer contents, int mode, VertexFormat format, int vertexCount, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        DrawCapture.immediate(contents, mode, format, vertexCount);
        callback.cancel();
    }
}
