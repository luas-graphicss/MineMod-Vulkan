package io.github.luasgraphicss.minemod.adapters.minecraft.mixin.opengl;

import com.mojang.blaze3d.platform.GlStateManager;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlStateManager.class)
abstract class BufferCallsMixin {
    @Inject(method = "_glGenBuffers", at = @At("HEAD"), cancellable = true)
    private static void minemod$nameABuffer(CallbackInfoReturnable<Integer> callback) {
        if (ClientRendering.replacesOpenGl()) {
            callback.setReturnValue(ClientRendering.openGl().buffers().generate());
        }
    }

    @Inject(
            method = {
                "_glBufferData(ILjava/nio/ByteBuffer;I)V",
                "_glBindBuffer",
                "_glDeleteBuffers",
                "_vertexAttribPointer",
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
