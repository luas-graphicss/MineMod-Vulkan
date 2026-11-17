package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.draw;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import io.github.luasgraphicss.minemod.adapters.minecraft.draw.DrawCapture;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferUploader.class)
abstract class BufferUploaderMixin {
    @Inject(method = "_drawWithShader", at = @At("HEAD"), cancellable = true)
    private static void minemod$recordTheDraw(BufferBuilder.RenderedBuffer rendered, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        try {
            BufferBuilder.DrawState drawn = rendered.drawState();
            DrawCapture.immediate(
                    rendered.vertexBuffer(), drawn.mode(), drawn.format(), drawn.vertexCount(), !drawn.sequentialIndex());
        } finally {
            rendered.release();
        }
        callback.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void minemod$leaveOutADrawWithoutItsShader(
            BufferBuilder.RenderedBuffer rendered, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        try {
            DrawCapture.leaveOut(rendered.drawState());
        } finally {
            rendered.release();
        }
        callback.cancel();
    }
}
