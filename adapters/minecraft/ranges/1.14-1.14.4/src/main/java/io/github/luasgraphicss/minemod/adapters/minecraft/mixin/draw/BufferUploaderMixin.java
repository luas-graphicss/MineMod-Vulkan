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
    @Inject(method = "end", at = @At("HEAD"), cancellable = true)
    private void minemod$recordTheDraw(BufferBuilder finished, CallbackInfo callback) {
        if (!GameFrame.isRecording()) {
            return;
        }
        DrawCapture.immediate(
                finished.getBuffer(), finished.getDrawMode(), finished.getVertexFormat(), finished.getVertexCount());
        finished.clear();
        callback.cancel();
    }
}
