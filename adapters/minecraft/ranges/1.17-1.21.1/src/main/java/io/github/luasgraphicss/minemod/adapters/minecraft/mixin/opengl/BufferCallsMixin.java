package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import java.nio.ByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class BufferCallsMixin {
    @Inject(method = {"_glGenBuffers", "_glGenVertexArrays"}, at = @At("HEAD"), cancellable = true)
    private static void minemod$nameABuffer(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().buffers().generate());
        }
    }

    @Inject(method = "_glBufferData(IJI)V", at = @At("HEAD"), cancellable = true)
    private static void minemod$sizeABuffer(int target, long bytes, int usage, CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            ClientRendering.openGl().buffers().resize(target, bytes);
            callback.cancel();
        }
    }

    @Inject(method = "_glMapBuffer", at = @At("HEAD"), cancellable = true)
    private static void minemod$mapABuffer(int target, int access, CallbackInfoReturnable<ByteBuffer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().buffers().map(target).orElse(null));
        }
    }

    @Inject(method = "_drawElements", at = @At("HEAD"), cancellable = true)
    private static void minemod$leaveOutADrawWithoutItsMesh(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            GameFrame.leaveOut();
            callback.cancel();
        }
    }

    @Inject(
            method = {
                "_glBufferData(ILjava/nio/ByteBuffer;I)V",
                "_glBindBuffer",
                "_glBindVertexArray",
                "_glUnmapBuffer",
                "_glDeleteBuffers",
                "_glDeleteVertexArrays",
                "_vertexAttribPointer",
                "_vertexAttribIPointer",
                "_enableVertexAttribArray",
                "_disableVertexAttribArray"
            },
            at = @At("HEAD"),
            cancellable = true)
    private static void minemod$leaveBuffersToTheScene(CallbackInfo callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.cancel();
        }
    }
}
